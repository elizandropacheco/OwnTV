#!/usr/bin/env python3
"""Private, authenticated snapshot/HLS adapter for an existing RTSP recorder.

Configuration and runtime data live outside the repository. No camera credentials
or request paths are written to logs. Requires Python 3.10+ and FFmpeg in PATH.
"""
import argparse
import hashlib
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import threading
import time
import urllib.parse
import urllib.request


class CameraBridge:
    def __init__(self, config, runtime):
        self.config = config
        self.recorder = config['recorder']
        self.channels = max(1, min(64, int(self.recorder.get('channels', 1))))
        self.tokens = config['tokens']
        if not self.tokens or any(not re.fullmatch(r'[-_A-Za-z0-9]{32,128}', t) for t in self.tokens):
            raise ValueError('Configure random access tokens with at least 32 characters.')
        self.allowed = set(config.get('allowed_clients', ['127.0.0.1']))
        self.runtime = runtime
        self.snapshots = {}
        self.snapshot_errors = {}
        self.snapshot_locks = {n: threading.Lock() for n in range(1, self.channels + 1)}
        self.streams = {}
        self.lock = threading.Lock()
        self.ffmpeg = shutil.which(config.get('ffmpeg', 'ffmpeg'))
        if not self.ffmpeg:
            raise ValueError('FFmpeg was not found.')
        self.closed = threading.Event()
        threading.Thread(target=self._reap, daemon=True).start()

    def authorized(self, address, token):
        return address in self.allowed and any(hmac.compare_digest(token, t) for t in self.tokens)

    def snapshot(self, channel):
        with self.snapshot_locks[channel]:
            previous = self.snapshots.get(channel)
            if previous and time.monotonic() - previous[0] < 2:
                return previous[1]
            if time.monotonic() - self.snapshot_errors.get(channel, -100) < 5:
                raise ValueError('Camera temporarily unavailable')
            r = self.recorder
            base = r.get('http_base') or ('http://' + r['host'] + ':' + str(r.get('http_port', 80)))
            manager = urllib.request.HTTPPasswordMgrWithDefaultRealm()
            manager.add_password(None, base, r['user'], r['password'])
            opener = urllib.request.build_opener(urllib.request.HTTPDigestAuthHandler(manager))
            try:
                with opener.open(base + '/cgi-bin/snapshot.cgi?channel=' + str(channel), timeout=8) as response:
                    data = response.read(2 * 1024 * 1024 + 1)
                    if not data.startswith(b'\xff\xd8') or len(data) > 2 * 1024 * 1024:
                        raise ValueError('Invalid snapshot')
            except Exception:
                self.snapshot_errors[channel] = time.monotonic()
                raise
            self.snapshots[channel] = (time.monotonic(), data)
            return data

    def stream(self, channel, quality, fit=None):
        with self.lock:
            key = (channel, quality, fit)
            current = self.streams.get(key)
            if current and current['process'].poll() is None:
                playlist = current['path'] / 'index.m3u8'
                fresh = (time.time() - playlist.stat().st_mtime < 20) if playlist.exists() else (time.monotonic() - current['created'] < 30)
                if fresh:
                    current['used'] = time.monotonic()
                    return current
            if current:
                self._stop(current)
                del self.streams[key]
            if len(self.streams) >= 12:
                raise ValueError('Live stream limit reached')
            r = self.recorder
            user, password = urllib.parse.quote(r['user'], safe=''), urllib.parse.quote(r['password'], safe='')
            uri = 'rtsp://' + user + ':' + password + '@' + r['host'] + ':' + str(r.get('port', 554))
            uri += '/cam/realmonitor?channel=' + str(channel) + '&subtype=' + ('0' if quality == 'main' else '1')
            folder = Path(tempfile.mkdtemp(prefix='live-', dir=self.runtime))
            # Explicit framing is normalized on the server: webOS native video
            # does not implement object-fit consistently. Legacy URLs can copy H.264.
            video = ['-c:v', 'copy']
            if fit or r.get('transcode', False):
                width, height = (1920, 1080) if quality == 'main' else (960, 540)
                filters = []
                if fit == 'cover':
                    filters = ['-vf', f'scale={width}:{height}:force_original_aspect_ratio=increase,crop={width}:{height},setsar=1']
                elif fit == 'contain':
                    filters = ['-vf', f'scale={width}:{height}:force_original_aspect_ratio=decrease,pad={width}:{height}:(ow-iw)/2:(oh-ih)/2,setsar=1']
                if config_encoder := self.config.get('encoder'):
                    if config_encoder != 'h264_videotoolbox':
                        raise ValueError('Unsupported hardware encoder')
                    video = filters + ['-c:v', config_encoder, '-b:v', '5M' if quality == 'main' else '1200k', '-realtime', '1', '-g', '50', '-pix_fmt', 'yuv420p']
                else:
                    video = filters + ['-c:v', 'libx264', '-threads', '2', '-preset', 'veryfast', '-tune', 'zerolatency', '-pix_fmt', 'yuv420p', '-g', '50']
            command = [self.ffmpeg, '-nostdin', '-hide_banner', '-loglevel', 'error', '-rtsp_transport', 'tcp', '-timeout', '15000000',
                       '-i', uri, '-map', '0:v:0', '-map', '0:a:0?'] + video + [
                       '-c:a', 'aac', '-b:a', '96k', '-f', 'hls', '-hls_time', '2', '-hls_list_size', '5',
                       '-hls_start_number_source', 'epoch', '-hls_flags', 'delete_segments+omit_endlist+discont_start', '-hls_segment_filename', str(folder / 'part%06d.ts'),
                       str(folder / 'index.m3u8')]
            process = subprocess.Popen(command, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            current = {'process': process, 'path': folder, 'used': time.monotonic(), 'created': time.monotonic()}
            self.streams[key] = current
            return current

    def _reap(self):
        while not self.closed.wait(5):
            with self.lock:
                for key, value in list(self.streams.items()):
                    if time.monotonic() - value['used'] > 35 or value['process'].poll() is not None:
                        self._stop(value)
                        del self.streams[key]

    @staticmethod
    def _stop(value):
        process = value['process']
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
        shutil.rmtree(value['path'], ignore_errors=True)

    def close(self):
        self.closed.set()
        with self.lock:
            for value in self.streams.values():
                self._stop(value)
            self.streams.clear()


def handler_for(bridge):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass  # URL paths contain bearer tokens; never log them.

        def reply(self, status, content, kind='application/json'):
            self.send_response(status)
            self.send_header('Content-Type', kind)
            self.send_header('Content-Length', str(len(content)))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('X-Content-Type-Options', 'nosniff')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            try:
                self.wfile.write(content)
            except (BrokenPipeError, ConnectionResetError):
                pass

        def do_GET(self):
            parsed = urllib.parse.urlsplit(self.path)
            match = re.fullmatch(r'/t/([-_A-Za-z0-9]{32,128})/(.+)', parsed.path)
            if not match or not bridge.authorized(self.client_address[0], match[1]):
                self.reply(403, b'{"error":"access denied"}')
                return
            route = match[2]
            if route == 'status':
                data = {'ready': True, 'channels': bridge.channels, 'snapshotInterval': 2,
                        'labels': ['Câmera ' + str(i) for i in range(1, bridge.channels + 1)]}
                self.reply(200, json.dumps(data).encode())
                return
            shot = re.fullmatch(r'snapshot/(\d+)\.jpg', route)
            live = re.fullmatch(r'live/(\d+)/(main|sub)/(?:(cover|contain)/)?(index\.m3u8|part\d{6,20}\.ts)', route)
            if not shot and not live:
                self.reply(404, b'{"error":"not found"}')
                return
            channel = int((shot or live)[1])
            if channel < 1 or channel > bridge.channels:
                self.reply(404, b'{"error":"channel unavailable"}')
                return
            try:
                if shot:
                    self.reply(200, bridge.snapshot(channel), 'image/jpeg')
                    return
                stream = bridge.stream(channel, live[2], live[3])
                target = stream['path'] / live[4]
                if live[4] == 'index.m3u8':
                    deadline = time.monotonic() + 14
                    while not target.is_file() and time.monotonic() < deadline and stream['process'].poll() is None:
                        time.sleep(0.15)
                if not target.is_file():
                    self.reply(503, b'{"error":"stream not ready"}')
                    return
                kind = 'application/vnd.apple.mpegurl' if live[4].endswith('.m3u8') else 'video/mp2t'
                self.reply(200, target.read_bytes(), kind)
            except Exception:
                self.reply(502, b'{"error":"camera unavailable"}')

    return Handler


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', required=True, type=Path)
    args = parser.parse_args()
    if os.name != 'nt' and args.config.stat().st_mode & 0o077:
        raise SystemExit('Restrict the private configuration: chmod 600 CONFIG')
    config = json.loads(args.config.read_text())
    bind = config.get('bind', '127.0.0.1')
    if bind in ('0.0.0.0', '::'):
        raise SystemExit('Bind to a specific local interface, not every network.')
    with tempfile.TemporaryDirectory(prefix='owntv-camera-') as temporary:
        bridge = CameraBridge(config, Path(temporary))
        server = ThreadingHTTPServer((bind, int(config.get('port', 8788))), handler_for(bridge))
        print('OwnTV camera bridge ready. Private access enabled.', flush=True)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass
        finally:
            server.server_close()
            bridge.close()


if __name__ == '__main__':
    main()
