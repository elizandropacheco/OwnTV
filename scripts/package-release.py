#!/usr/bin/env python3
"""Build, lint, verify and stage OwnTV with Python's standard library only."""

import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parents[1]


def fail(message):
    raise RuntimeError(message)


def run(args, *, capture=False):
    return subprocess.run(
        [str(arg) for arg in args], cwd=ROOT, check=True,
        text=True, stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
    ).stdout


def gradle_value(text, name):
    match = re.search(rf'^\s*{re.escape(name)}\s*=\s*(?:"([^"]+)"|(\d+))', text, re.M)
    if not match:
        fail(f"Não foi possível ler {name} em app/build.gradle.kts.")
    return match.group(1) or int(match.group(2))


def sdk_directory():
    value = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if value:
        return Path(value).expanduser()
    properties = ROOT / "local.properties"
    if properties.is_file():
        match = re.search(r"^sdk\.dir=(.+)$", properties.read_text(), re.M)
        if match:
            return Path(match.group(1).replace("\\:", ":").replace("\\\\", "\\"))
    fail("Defina ANDROID_HOME ou sdk.dir em local.properties para localizar o Android SDK.")


def source_state():
    try:
        top = Path(run(["git", "rev-parse", "--show-toplevel"], capture=True).strip()).resolve()
        if top != ROOT:
            return None, None
        commit = run(["git", "rev-parse", "HEAD"], capture=True).strip()
        # Release output is not source input, so its metadata does not dirty the build.
        dirty = bool(run([
            "git", "status", "--porcelain", "--untracked-files=normal", "--",
            ".", ":(exclude)releases",
        ], capture=True).strip())
        return commit, dirty
    except (FileNotFoundError, subprocess.CalledProcessError):
        return None, None


def main():
    parser = argparse.ArgumentParser(
        description="Compila release, executa lint, verifica o APK e gera releases/<versão>/.",
        epilog="Sem chave externa ou --local-sign, gera um APK unsigned, não instalável.",
    )
    parser.add_argument("--local-sign", action="store_true",
                        help="Usa a chave debug local já existente; apenas desenvolvimento/compatibilidade.")
    parser.add_argument("--clean", action="store_true", help="Executa clean antes da compilação.")
    parser.add_argument("--offline", action="store_true", help="Exige dependências já presentes no cache Gradle.")
    args = parser.parse_args()

    run([sys.executable, ROOT / "scripts/check-public-assets.py"])

    build = (ROOT / "app/build.gradle.kts").read_text()
    version = gradle_value(build, "versionName")
    code = gradle_value(build, "versionCode")
    app_id = gradle_value(build, "applicationId")
    build_tools = gradle_value(build, "buildToolsVersion")
    if not isinstance(version, str) or not re.fullmatch(r"\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?", version):
        fail("versionName deve ser uma versão segura, como 0.2.3 ou 0.2.3-beta.1.")

    sdk = sdk_directory()
    extension = ".bat" if os.name == "nt" else ""
    apksigner = sdk / "build-tools" / build_tools / ("apksigner" + extension)
    aapt = sdk / "build-tools" / build_tools / ("aapt.exe" if os.name == "nt" else "aapt")
    for executable in (apksigner, aapt):
        if not executable.is_file():
            fail(f"Instale Android SDK Build Tools {build_tools}; falta {executable.name}.")

    signing_env = ("KEYSTORE_FILE", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
    external_signing = bool(os.environ.get("KEYSTORE_FILE"))
    if any(os.environ.get(name) for name in signing_env) and not all(os.environ.get(name) for name in signing_env):
        fail("Para assinar, defina todas: KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS e KEY_PASSWORD.")
    if args.local_sign and external_signing:
        fail("Escolha --local-sign ou a chave por variáveis de ambiente; não os dois.")
    if external_signing:
        key_file = Path(os.environ["KEYSTORE_FILE"]).expanduser()
        if not key_file.is_absolute() or not key_file.is_file():
            fail("KEYSTORE_FILE deve apontar para um arquivo existente com caminho absoluto.")
    if args.local_sign:
        android_user = Path(os.environ.get("ANDROID_USER_HOME", str(Path.home() / ".android")))
        if not (android_user / "debug.keystore").is_file():
            fail("--local-sign exige a chave debug já existente. Não gere outra para atualizar uma TV instalada.")

    wallpapers = sorted((ROOT / "app/src/main/assets/ep/wallpapers").glob("*.jpg"))
    if wallpapers:
        run([sys.executable, ROOT / "scripts/sanitize-jpeg.py", *wallpapers])

    commit_before, dirty_before = source_state()
    started = datetime.datetime.now(datetime.timezone.utc)
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    command = [wrapper]
    if args.clean:
        command.append("clean")
    command += [":app:assembleRelease", ":app:lintRelease", "--no-daemon", "--console=plain"]
    if args.local_sign:
        command.append("-PlocalSign")
    if args.offline:
        command.append("--offline")
    print(f"Compilando OwnTV {version} ({code}) e executando lintRelease…", flush=True)
    run(command)

    output_dir = ROOT / "app/build/outputs/apk/release"
    metadata = json.loads((output_dir / "output-metadata.json").read_text())
    elements = metadata.get("elements", [])
    if metadata.get("applicationId") != app_id or len(elements) != 1:
        fail("Esperado um APK universal com o applicationId atual; confira output-metadata.json.")
    element = elements[0]
    if element.get("versionCode") != code or element.get("versionName") != version:
        fail("A versão do APK não corresponde ao código atual; a entrega foi interrompida.")
    output_file = element.get("outputFile", "")
    if not output_file or Path(output_file).name != output_file:
        fail("Nome de APK inválido no output-metadata.json.")
    apk = output_dir / output_file
    badging = run([aapt, "dump", "badging", apk], capture=True)
    if "application-debuggable" in badging:
        fail("O APK é depurável; não será entregue como release.")
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.M)
    if not package or package.groups() != (app_id, str(code), version):
        fail("O manifesto interno do APK diverge da versão ou do pacote esperado.")

    verify = subprocess.run(
        [str(apksigner), "verify", "--verbose", "--print-certs", str(apk)],
        cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )
    signed = verify.returncode == 0
    expected_signed = external_signing or args.local_sign
    if signed != expected_signed:
        fail("A assinatura do APK diverge do modo solicitado; a entrega foi interrompida.")
    certificates = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$", verify.stdout, re.M)
    if signed and not certificates:
        fail("O APK verificou, mas o SHA-256 do certificado não foi encontrado.")
    # A failing verifier is acceptable only for a genuinely unsigned APK.
    if not signed and not re.search(r"(?:Missing META-INF/MANIFEST.MF|No signatures)", verify.stderr + verify.stdout, re.I):
        fail("apksigner falhou por um motivo diferente de ausência de assinatura.")

    commit_after, dirty_after = source_state()
    if commit_before != commit_after:
        fail("O commit mudou durante a compilação; execute novamente para registrar a origem correta.")
    filename = f"OwnTV-{version}{'' if signed else '-unsigned'}.apk"
    hasher = hashlib.sha256()
    with apk.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            hasher.update(chunk)
    digest = hasher.hexdigest()
    record = {
        "schemaVersion": 1,
        "applicationId": app_id,
        "versionName": version,
        "versionCode": code,
        "sourceCommit": commit_after,
        "sourceDirty": None if dirty_before is None else bool(dirty_before or dirty_after),
        "buildStartedAtUtc": started.isoformat(timespec="seconds"),
        "buildFinishedAtUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "buildType": "release",
        "debuggable": False,
        "validation": {"assembleRelease": "passed", "lintRelease": "passed", "apkManifest": "passed"},
        "signing": {
            "signed": signed,
            "mode": "environment-keystore" if external_signing else "local-debug-key" if args.local_sign else "unsigned",
            "verification": "passed" if signed else "unsigned",
            "certificateSha256": [value.lower() for value in certificates],
        },
        "android": {
            "minSdk": gradle_value(build, "minSdk"),
            "targetSdk": gradle_value(build, "targetSdk"),
            "compileSdk": gradle_value(build, "compileSdk"),
            "buildTools": build_tools,
        },
        "bundledWallpapers": len(list((ROOT / "app/src/main/assets/ep/wallpapers").glob("*.jpg"))),
        "optionalGameArtwork": bool(list((ROOT / "app/src/main/assets/ep/cards").glob("fortnite*.jpg"))),
        "artifact": {"file": filename, "bytes": apk.stat().st_size, "sha256": digest},
    }

    release_dir = ROOT / "releases" / version
    release_dir.mkdir(parents=True, exist_ok=True)
    # Never replace the previous deliverable until the new build has passed all checks.
    with tempfile.TemporaryDirectory(prefix=".stage-", dir=release_dir) as temporary:
        stage = Path(temporary)
        shutil.copyfile(apk, stage / filename)
        (stage / "SHA256SUMS.txt").write_text(f"{digest}  {filename}\n")
        (stage / "build-info.json").write_text(json.dumps(record, indent=2, ensure_ascii=False) + "\n")
        for source in stage.iterdir():
            os.replace(source, release_dir / source.name)
    alternate = release_dir / f"OwnTV-{version}{'-unsigned' if signed else ''}.apk"
    if alternate.is_file():
        alternate.unlink()
    print(f"Entrega: releases/{version}/{filename}")
    print(f"SHA-256: {digest}")
    print("Metadados: build-info.json · Integridade: SHA256SUMS.txt")
    if not signed:
        print("APK unsigned: assine antes de instalar. Veja docs/BUILDING.md.")
    elif args.local_sign:
        print("Assinatura de desenvolvimento local: preserve esta chave para atualizações compatíveis.")
    if dirty_before or dirty_after:
        print("Origem com alterações locais; sourceDirty=true registra isso sem ocultar modificações.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, ValueError, KeyError, subprocess.CalledProcessError) as error:
        print(f"Erro: {error}", file=sys.stderr)
        sys.exit(1)
