#!/usr/bin/env python3
"""Fail before publishing editable personal marks or private signing material.

This is a targeted release guard, not a general secret scanner. Private files
must remain outside the checkout even if ignored by Git.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
PRIVATE_EXTENSIONS = {'.psd', '.ai', '.xcf', '.cdr', '.jks', '.keystore', '.pem', '.p12', '.pfx'}
LOGO_RESOURCES = {'own_mark', 'own_ep_mark', 'own_monogram', 'own_ep_monogram',
                  'own_ep_animated', 'own_mark_animated', 'app_banner'}


def check():
    failures = []
    result = subprocess.run(['git', '-C', str(ROOT), 'rev-parse', '--show-toplevel'],
                            capture_output=True, text=True)
    has_repo = result.returncode == 0 and Path(result.stdout.strip()).resolve() == ROOT
    if has_repo:
        names = subprocess.check_output(['git', '-C', str(ROOT), 'ls-files', '-z',
                                         '--cached', '--others', '--exclude-standard'])
        paths = [ROOT / name.decode() for name in names.split(b'\0') if name]
    else:
        paths = [p for p in ROOT.rglob('*') if p.is_file() and not any(
            part in {'.git', '.gradle', '.kotlin', 'build', '__pycache__'}
            for part in p.relative_to(ROOT).parts)]
    for path in paths:
        if not path.is_file():
            continue
        rel = path.relative_to(ROOT).as_posix()
        if path.suffix.lower() in PRIVATE_EXTENSIONS:
            failures.append(rel + ': private master or signing file')
        if rel.startswith('art/owntv/') or rel == 'app/src/main/assets/ep/logo.png':
            failures.append(rel + ': original brand source')
        if 'app/src/main/res/' in rel and path.suffix == '.xml' and path.stem in LOGO_RESOURCES:
            failures.append(rel + ': publish a rendered PNG resource instead')
        if path.suffix.lower() == '.svg' and ('branding/' in rel or 'owntv' in path.name.lower()
                                             or path.name.lower().startswith('ep-')):
            failures.append(rel + ': editable personal mark')
        if path.name == 'OwnBrand.java':
            source = path.read_text()
            if 'createPathFromPathData' in source or 'PathParser' in source:
                failures.append(rel + ': private geometry must not be embedded in code')
    # Check these known ignored locations too, so a local master cannot enter packaging by mistake.
    if (ROOT / 'app/src/main/assets/ep/logo.png').exists():
        failures.append('app/src/main/assets/ep/logo.png: ignored original still present')
    if (ROOT / 'art/owntv').exists() and any((ROOT / 'art/owntv').iterdir()):
        failures.append('art/owntv/: ignored masters still present')
    if failures:
        print('Public asset check failed:\n' + '\n'.join(sorted(set(failures))), file=sys.stderr)
        return 1
    print('Public asset check passed: no editable personal marks or tracked signing files.')
    return 0


if __name__ == '__main__':
    sys.exit(check())
