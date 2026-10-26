#!/usr/bin/env bash
# Optional developer tool; not required to build the application.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec python3 - "$repo_dir" "$@" <<'PY'
import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import urllib.request

root = Path(sys.argv[1])
parser = argparse.ArgumentParser(description="Format Java and Kotlin sources with pinned official tools.")
parser.add_argument("--check", action="store_true", help="Check formatting without changing files")
parser.add_argument("--download", action="store_true", help="Download missing formatter JARs from official releases")
parser.add_argument("--tools-dir", type=Path, default=root / "build" / "formatters", help="Formatter cache; never commit it")
args = parser.parse_args(sys.argv[2:])
tools = [
    (
        "google-java-format-1.37.0-all-deps.jar",
        "https://github.com/google/google-java-format/releases/download/v1.37.0/google-java-format-1.37.0-all-deps.jar",
        "834b2a0c38cb774953322a84b5ca3f2f40dd3156650b3cd44d3b744345962f7a",
        "*.java",
        ["--aosp", "--skip-sorting-imports", "--skip-removing-unused-imports", "--skip-reflowing-long-strings", "--skip-reordering-modifiers"],
    ),
    (
        "ktfmt-0.65-with-dependencies.jar",
        "https://github.com/Kotlin/ktfmt/releases/download/v0.65/ktfmt-0.65-with-dependencies.jar",
        "aba9753b6ab387926179ab7318ba8e0c33dad483ebc72f633e79522265b15214",
        "*.kt",
        ["--kotlinlang-style", "--do-not-remove-unused-imports", "--quiet"],
    ),
]
java = str(Path(os.environ["JAVA_HOME"]) / "bin" / "java") if os.environ.get("JAVA_HOME") else "java"
try:
    for name, url, expected, pattern, options in tools:
        jar = args.tools_dir / name
        if not jar.is_file():
            if not args.download:
                parser.error(f"Missing {name}; use --download or --tools-dir with the official JARs")
            jar.parent.mkdir(parents=True, exist_ok=True)
            temporary = jar.with_suffix(".download")
            try:
                with urllib.request.urlopen(url, timeout=60) as response, temporary.open("wb") as output:
                    while chunk := response.read(1024 * 1024):
                        output.write(chunk)
                if hashlib.sha256(temporary.read_bytes()).hexdigest() != expected:
                    raise RuntimeError(f"Checksum mismatch for {name}")
                temporary.replace(jar)
            finally:
                temporary.unlink(missing_ok=True)
        if hashlib.sha256(jar.read_bytes()).hexdigest() != expected:
            raise RuntimeError(f"Checksum mismatch for {name}; refusing to execute")
        sources = sorted(str(path) for path in (root / "app/src/main/java").rglob(pattern))
        if not sources:
            continue
        command = [java, "-jar", str(jar), *options]
        command += ["--dry-run", "--set-exit-if-changed"] if args.check else (["--replace"] if pattern == "*.java" else [])
        subprocess.run([*command, *sources], check=True)
    print("Formatting check passed." if args.check else "Sources formatted.")
except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
    print(f"Formatting failed: {error}", file=sys.stderr)
    sys.exit(1)
PY
