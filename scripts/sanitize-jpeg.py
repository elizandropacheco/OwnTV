#!/usr/bin/env python3
"""Remove capture metadata from local JPEG/PNG photos without recompression."""

import argparse
from pathlib import Path
import sys
from jpeg_privacy import MAX_IMAGE_BYTES, replace_atomically, sanitize_photo


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("files", type=Path, nargs="+")
    args = parser.parse_args()
    for target in args.files:
        if target.is_symlink() or target.stat().st_size > MAX_IMAGE_BYTES:
            raise ValueError("Refusing symlink or JPEG exceeding 35 MiB")
        original = target.read_bytes()
        clean = sanitize_photo(original)
        if clean != original:
            replace_atomically(target, clean)
        print(f"Sanitized {target.name}: {len(original) - len(clean)} metadata bytes removed")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError) as error:
        print(f"Sanitization failed: {error}", file=sys.stderr)
        sys.exit(1)
