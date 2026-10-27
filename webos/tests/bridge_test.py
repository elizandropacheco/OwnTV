import importlib.util
from pathlib import Path
import tempfile
import threading
import unittest
import urllib.request
import urllib.error
from http.server import ThreadingHTTPServer

spec=importlib.util.spec_from_file_location('bridge',Path(__file__).parents[1]/'bridge/camera_bridge.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
class Fake:
    channels=2
    def authorized(self,ip,token):return ip=='127.0.0.1' and token=='a'*32
    def snapshot(self,n):return b'\xff\xd8test'
class Tests(unittest.TestCase):
    def setUp(self):
        self.server=ThreadingHTTPServer(('127.0.0.1',0),module.handler_for(Fake()))
        self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start()
        self.base='http://127.0.0.1:'+str(self.server.server_port)
    def tearDown(self):self.server.shutdown();self.server.server_close()
    def status(self,path):
        try:
            with urllib.request.urlopen(self.base+path) as r:return r.status,r.read()
        except urllib.error.HTTPError as e:
            try:return e.code,e.read()
            finally:e.close()
    def test_authorization_and_routes(self):
        prefix='/t/'+'a'*32
        self.assertEqual(self.status('/status')[0],403)
        self.assertEqual(self.status('/t/'+'b'*32+'/status')[0],403)
        self.assertEqual(self.status(prefix+'/status')[0],200)
        self.assertEqual(self.status(prefix+'/snapshot/2.jpg'),(200,b'\xff\xd8test'))
        self.assertEqual(self.status(prefix+'/snapshot/3.jpg')[0],404)
        self.assertEqual(self.status(prefix+'/../camera-private.json')[0],404)
        self.assertEqual(self.status(prefix+'/live/1/main/../../secrets')[0],404)

class StreamRecoveryTests(unittest.TestCase):
    def test_stale_playlist_restarts_with_unique_sequence_and_bounded_io(self):
        import os
        import time
        from unittest.mock import patch
        processes = []
        commands = []

        class Process:
            stopped = False
            def poll(self): return 0 if self.stopped else None
            def terminate(self): self.stopped = True
            def wait(self, timeout=None): return 0
            def kill(self): self.stopped = True

        def start(command, **kwargs):
            commands.append(command)
            process = Process()
            processes.append(process)
            return process

        config = {'recorder': {'channels': 1, 'host': 'recorder.example.invalid',
                   'user': 'example', 'password': 'example'}, 'tokens': ['a' * 40]}
        with tempfile.TemporaryDirectory() as directory, \
             patch.object(module.shutil, 'which', return_value='/example/ffmpeg'), \
             patch.object(module.subprocess, 'Popen', side_effect=start):
            bridge = module.CameraBridge(config, Path(directory))
            try:
                first = bridge.stream(1, 'main', 'cover')
                playlist = first['path'] / 'index.m3u8'
                playlist.write_text('#EXTM3U')
                self.assertIs(bridge.stream(1, 'main', 'cover'), first)
                os.utime(playlist, (time.time() - 30, time.time() - 30))
                second = bridge.stream(1, 'main', 'cover')
                self.assertIsNot(second, first)
                self.assertTrue(processes[0].stopped)
                self.assertIn('-timeout', commands[1])
                self.assertIn('epoch', commands[1])
                self.assertIn('force_original_aspect_ratio=increase', ' '.join(commands[1]))
            finally:
                bridge.close()

if __name__ == "__main__":
    unittest.main()
