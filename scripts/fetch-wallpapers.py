#!/usr/bin/env python3
"""Fetch credited full-resolution photos, removing capture metadata losslessly."""

import argparse
import json
from pathlib import Path
import re
import sys
import urllib.parse
import urllib.request
from jpeg_privacy import MAX_IMAGE_BYTES, replace_atomically, sanitize_photo


def validate_url(url):
    parsed = urllib.parse.urlsplit(url)
    if (parsed.scheme != "https" or parsed.hostname != "upload.wikimedia.org"
            or parsed.username is not None or parsed.password is not None
            or parsed.port not in (None, 443)):
        raise ValueError("Wallpaper downloads require HTTPS on upload.wikimedia.org")
    return url


class SafeRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        validate_url(new_url)
        return super().redirect_request(request, response, code, message, headers, new_url)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--count", type=int, default=15)
    args = parser.parse_args()
    if not 0 <= args.count <= 100:
        parser.error("--count must be between 0 and 100")
    root = Path(__file__).resolve().parents[1] / "app/src/main/assets/ep/wallpapers"
    catalog = json.loads((root / "catalog.json").read_text())
    opener = urllib.request.build_opener(SafeRedirect())
    for photo in catalog[:args.count]:
        identifier = photo["id"]
        if not isinstance(identifier, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,80}", identifier):
            raise ValueError("Invalid wallpaper identifier")
        target = root / (identifier + ".jpg")
        if target.is_symlink():
            raise ValueError("Wallpaper target cannot be a symlink")
        if target.exists():
            if target.stat().st_size > MAX_IMAGE_BYTES:
                raise ValueError("Existing wallpaper exceeds 35 MiB")
            original = target.read_bytes()
            clean = sanitize_photo(original)
            if original != clean:
                replace_atomically(target, clean)
            print(f"Ready: {identifier}")
            continue
        request = urllib.request.Request(validate_url(photo["url"]), headers={
            "User-Agent": "OwnTV wallpaper setup (https://elizandropacheco.io)"
        })
        with opener.open(request, timeout=30) as response:
            validate_url(response.url)
            length = response.headers.get("Content-Length")
            if length is not None and int(length) > MAX_IMAGE_BYTES:
                raise ValueError("Wallpaper exceeds 35 MiB")
            data = bytearray()
            while chunk := response.read(min(1024 * 1024, MAX_IMAGE_BYTES + 1 - len(data))):
                data += chunk
                if len(data) > MAX_IMAGE_BYTES:
                    raise ValueError("Wallpaper exceeds 35 MiB")
        replace_atomically(target, sanitize_photo(bytes(data)))
        print(f"Downloaded: {identifier}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"Wallpaper setup failed: {error}", file=sys.stderr)
        sys.exit(1)
