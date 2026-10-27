#!/usr/bin/env python3
"""Install the private camera bridge as the current user's macOS LaunchAgent."""
import argparse
import json
import os
from pathlib import Path
import plistlib
import shutil
import subprocess
import sys

LABEL = 'io.elizandropacheco.owntv.camera-bridge'

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', required=True, type=Path)
    args = parser.parse_args()
    if sys.platform != 'darwin':
        raise SystemExit('This installer is for macOS. See the bridge guide for Linux.')
    if args.config.stat().st_mode & 0o077:
        raise SystemExit('Restrict the configuration first: chmod 600 CONFIG')
    config = json.loads(args.config.read_text())
    if not config.get('tokens') or not config.get('recorder') or config.get('bind') in (None, '0.0.0.0', '::'):
        raise SystemExit('Complete the private configuration before installing.')
    executable = shutil.which(config.get('ffmpeg', 'ffmpeg'))
    if not executable:
        raise SystemExit('Install FFmpeg before installing the bridge.')
    config['ffmpeg'] = executable
    destination = Path.home() / 'Library/Application Support/OwnTV/bridge'
    destination.mkdir(parents=True, exist_ok=True, mode=0o700)
    destination.chmod(0o700)
    shutil.copyfile(Path(__file__).with_name('camera_bridge.py'), destination / 'camera_bridge.py')
    private = destination / 'private.json'
    fd = os.open(private, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'w') as stream:
        json.dump(config, stream)
    private.chmod(0o600)
    agents = Path.home() / 'Library/LaunchAgents'
    agents.mkdir(parents=True, exist_ok=True)
    target = agents / (LABEL + '.plist')
    data = {'Label': LABEL, 'ProgramArguments': [sys.executable, str(destination / 'camera_bridge.py'),
             '--config', str(private)], 'RunAtLoad': True, 'KeepAlive': True,
            'ThrottleInterval': 30, 'WorkingDirectory': str(destination),
            'StandardOutPath': str(destination / 'service.log'),
            'StandardErrorPath': str(destination / 'service-error.log')}
    target.write_bytes(plistlib.dumps(data)); target.chmod(0o600)
    domain = 'gui/' + str(os.getuid())
    subprocess.run(['launchctl', 'bootout', domain + '/' + LABEL], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    subprocess.run(['launchctl', 'bootstrap', domain, str(target)], check=True)
    print('OwnTV camera bridge installed. It starts at user login and restarts if it exits.')
    print('Private files:', destination)

if __name__ == '__main__':
    main()
