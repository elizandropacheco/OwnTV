#!/usr/bin/env python3
"""Prepare Full HD derivatives of the credited Android wallpaper collection."""
import argparse,json
from pathlib import Path
from PIL import Image,ImageOps
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT/'app/src/main/assets/ep/wallpapers');args=p.parse_args()
catalog=json.loads((ROOT/'app/src/main/assets/ep/wallpapers/catalog.json').read_text())
out=ROOT/'webos/app/assets/wallpapers';out.mkdir(parents=True,exist_ok=True)
entries=[]
for item in catalog[:15]:
 src=args.source/(item['id']+'.jpg')
 if not src.exists():raise SystemExit('Missing wallpaper: '+src.name+'. Run scripts/fetch-wallpapers.py --count 15 first.')
 with Image.open(src) as original:
  img=ImageOps.exif_transpose(original).convert('RGB');img.thumbnail((1920,1080),Image.Resampling.LANCZOS);img.save(out/src.name,quality=88,optimize=True)
 entries.append(dict(item,file='assets/wallpapers/'+src.name,modification='Resized to fit 1920x1080; metadata removed; original aspect ratio preserved.'))
(ROOT/'webos/app/assets/wallpapers.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2)+'\n')
(ROOT/'webos/app/wallpapers.js').write_text('window.OwnTVWallpapers = '+json.dumps(entries,ensure_ascii=False)+';\n')
print('Prepared',len(entries),'credited wallpapers for Full HD.')
