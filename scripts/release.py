"""Local signing identity and verified release builds; never prints credentials."""

import argparse
import hashlib
import os
from pathlib import Path
import re
import secrets
import ssl
import stat
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / ".env.release-signing"
FIELDS = ("RECIPEGRABBER_KEYSTORE_FILE", "RECIPEGRABBER_STORE_PASSWORD",
          "RECIPEGRABBER_KEY_ALIAS", "RECIPEGRABBER_KEY_PASSWORD")


def private_file(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("Signing file is missing or is a symlink")
    info = path.stat()
    if info.st_uid != os.getuid() or stat.S_IMODE(info.st_mode) & 0o077:
        raise ValueError("Signing files must be owner-only (chmod 600)")


def load_config(path=CONFIG):
    private_file(path)
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        name, separator, value = line.partition("=")
        if not separator or name not in FIELDS or name in values or not value or any(ord(c) < 32 for c in value):
            raise ValueError("Invalid signing configuration")
        values[name] = value
    if set(values) != set(FIELDS):
        raise ValueError("Signing configuration is incomplete")
    key = Path(values[FIELDS[0]])
    if not key.is_absolute():
        raise ValueError("Keystore path must be absolute")
    private_file(key)
    return values


def run(arguments, env):
    result = subprocess.run(arguments, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise RuntimeError("{} failed (exit {}); output withheld to protect signing data".format(Path(str(arguments[0])).name, result.returncode))
    return result.stdout


def java_tool(name):
    home = os.environ.get("JAVA_HOME")
    if not home or not (Path(home) / "bin" / name).is_file():
        raise ValueError("Set JAVA_HOME to JDK 17")
    return str(Path(home) / "bin" / name)


def initialize():
    if CONFIG.exists() or CONFIG.is_symlink():
        load_config()
        print("Existing signing identity retained")
        return
    directory = Path.home() / ".local/share/recipegrabber-signing"
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    if directory.is_symlink() or directory.stat().st_uid != os.getuid() or stat.S_IMODE(directory.stat().st_mode) & 0o077:
        raise ValueError("Signing directory must be owner-only")
    key = directory / "release.p12"
    if key.exists() or key.is_symlink():
        raise ValueError("Keystore already exists; recover its config instead of replacing the identity")
    password = secrets.token_urlsafe(48)
    values = dict(zip(FIELDS, (str(key), password, "recipegrabber-release", password)))
    environment = dict(os.environ, **values)
    # keytool creates the file with the process umask; restore it immediately afterwards.
    previous_mask = os.umask(0o077)
    try:
        run([java_tool("keytool"), "-genkeypair", "-keystore", str(key), "-storetype", "PKCS12",
             "-storepass:env", FIELDS[1], "-keypass:env", FIELDS[3], "-alias", values[FIELDS[2]],
             "-keyalg", "RSA", "-keysize", "3072", "-validity", "10000", "-dname", "CN=RecipeGrabberV2", "-noprompt"], environment)
        descriptor = os.open(str(CONFIG), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(descriptor, "w", encoding="utf-8") as output:
            output.write("".join("{}={}\n".format(name, values[name]) for name in FIELDS))
            output.flush()
            os.fsync(output.fileno())
    finally:
        os.umask(previous_mask)
    load_config()
    print("Created private release identity; preserve both the keystore and ignored signing env securely")


def certificate_digest(env):
    certificate = run([java_tool("keytool"), "-exportcert", "-keystore", env[FIELDS[0]],
                       "-storepass:env", FIELDS[1], "-alias", env[FIELDS[2]]], env)
    return hashlib.sha256(certificate).hexdigest()


def verify(apk, env, expected_package="com.recipegrabber"):
    sdk = Path(env.get("ANDROID_HOME", env.get("ANDROID_SDK_ROOT", "")))
    tools = sdk / "build-tools/34.0.0"
    expected = certificate_digest(env)
    verification = run([str(tools / "apksigner"), "verify", "--verbose", "--print-certs", str(apk)], env).decode("utf-8")
    digests = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]+)", verification)
    if digests != [expected]:
        raise ValueError("APK signer does not match the configured release identity")
    manifest = run([str(tools / "aapt"), "dump", "badging", str(apk)], env).decode("utf-8")
    if "application-debuggable" in manifest:
        raise ValueError("Release APK must not be debuggable")
    package = re.search(r"package: name='([^']+)'", manifest)
    if package is None or package.group(1) != expected_package:
        raise ValueError("Unexpected release application ID")
    print("Verified non-debuggable APK: {}".format(apk.name))
    print("Release certificate SHA-256: {}".format(expected))
    print("APK SHA-256: {}".format(hashlib.sha256(apk.read_bytes()).hexdigest()))


def verify_bundle(bundle, env):
    expected = certificate_digest(env)
    run([java_tool("jarsigner"), "-verify", str(bundle)], env)
    certificates = run([java_tool("keytool"), "-printcert", "-jarfile", str(bundle), "-rfc"], env).decode("utf-8")
    blocks = re.findall(r"-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----", certificates, flags=re.S)
    digests = [hashlib.sha256(ssl.PEM_cert_to_DER_cert(block)).hexdigest() for block in blocks]
    if digests != [expected]:
        raise ValueError("Bundle signer does not match the configured release identity")
    print("Verified signed bundle: {}".format(bundle.name))
    print("AAB SHA-256: {}".format(hashlib.sha256(bundle.read_bytes()).hexdigest()))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("init-key", "build", "verify"))
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--package", default="com.recipegrabber", choices=("com.recipegrabber", "com.recipegrabber.releasecheck"))
    args = parser.parse_args()
    if args.command == "init-key":
        initialize()
        return
    environment = dict(os.environ, **load_config())
    if args.command == "verify":
        if args.apk is None:
            parser.error("verify requires --apk")
        verify(args.apk.resolve(), environment, args.package)
        return
    arguments = [str(ROOT / "gradlew"), "-PrequireReleaseSigning=true", "testDebugUnitTest", "lintDebug",
                 "assembleRelease", "bundleRelease", "assembleReleaseCheck", "--console=plain"]
    if args.offline:
        arguments.append("--offline")
    # Gradle output is intentionally not mirrored: private signing errors can contain secrets.
    run(arguments, environment)
    for relative, package in (("release/app-release.apk", "com.recipegrabber"),
                              ("releaseCheck/app-releaseCheck.apk", "com.recipegrabber.releasecheck")):
        verify(ROOT / "app/build/outputs/apk" / relative, environment, package)
    verify_bundle(ROOT / "app/build/outputs/bundle/release/app-release.aab", environment)
    print("Unit tests, lint and signed APK/AAB builds completed")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, RuntimeError, OSError):
        print("Release operation failed; check private config, JDK/SDK paths and local build reports. No credential values are printed.", file=sys.stderr)
        sys.exit(1)
