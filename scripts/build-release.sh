#!/usr/bin/env bash
# One entry point for a verified APK and its release metadata.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec python3 "$SCRIPT_DIR/package-release.py" "$@"
