#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PACKAGER="${OWNTV_ARES_PACKAGE:-ares-package}"
VERSION="$(node -p 'require(process.argv[1]).version' "$ROOT/webos/app/appinfo.json")"
OUT="$ROOT/releases/webos/$VERSION"
python3 "$ROOT/scripts/check-public-assets.py"
node --check "$ROOT/webos/app/app.js"
node "$ROOT/webos/tests/model.test.cjs"
python3 - "$ROOT" <<'VERIFY'
import json,sys
from pathlib import Path
root=Path(sys.argv[1]);app=root/'webos/app'
photos=json.loads((app/'assets/wallpapers.json').read_text())
if len(photos)!=15 or any(not (app/p['file']).is_file() for p in photos):
    raise SystemExit('Prepare the 15 webOS wallpapers before building.')
VERIFY
mkdir -p "$OUT"
"$PACKAGER" "$ROOT/webos/app" --outdir "$OUT"
python3 - "$ROOT" "$OUT" <<'PY'
import hashlib,json,sys
from pathlib import Path
root,out=map(Path,sys.argv[1:]);app=json.loads((root/'webos/app/appinfo.json').read_text())
ipk=out/(app['id']+'_'+app['version']+'_all.ipk')
if not ipk.is_file(): raise SystemExit('Expected IPK was not produced')
sha=hashlib.sha256(ipk.read_bytes()).hexdigest()
(out/'SHA256SUMS').write_text(sha+'  '+ipk.name+'\n')
(out/'build-info.json').write_text(json.dumps({'id':app['id'],'version':app['version'],'platform':'webOS','stage':'pilot','sha256':sha},indent=2)+'\n')
print('Package:',ipk)
PY
