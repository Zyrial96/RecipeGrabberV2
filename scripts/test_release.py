import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import hashlib
import subprocess

from release import FIELDS, load_config, verify, verify_bundle, run


class SigningConfigTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        self.key = self.directory / "fixture.p12"
        self.key.touch(mode=0o600)
        self.config = self.directory / ".env.fixture"
        self.values = dict(zip(FIELDS, (str(self.key), "synthetic-password", "fixture", "synthetic-password")))

    def write(self, values=None):
        self.config.write_text("".join("{}={}\n".format(k, v) for k, v in (values or self.values).items()))
        self.config.chmod(0o600)

    def test_complete_private_configuration(self):
        self.write()
        self.assertEqual(self.values, load_config(self.config))

    def test_missing_configuration_fails(self):
        with self.assertRaises(ValueError):
            load_config(self.config)

    def test_partial_and_duplicate_configuration_fail(self):
        self.write({FIELDS[0]: str(self.key)})
        with self.assertRaises(ValueError):
            load_config(self.config)
        self.write()
        with self.config.open("a") as output:
            output.write("{}=synthetic-duplicate\n".format(FIELDS[1]))
        with self.assertRaises(ValueError):
            load_config(self.config)

    def test_world_readable_config_or_key_fails(self):
        self.write()
        self.config.chmod(0o644)
        with self.assertRaises(ValueError):
            load_config(self.config)
        self.config.chmod(0o600)
        self.key.chmod(0o644)
        with self.assertRaises(ValueError):
            load_config(self.config)

    def test_symlink_and_relative_keystore_fail(self):
        self.write()
        link = self.directory / ".env.link"
        link.symlink_to(self.config)
        with self.assertRaises(ValueError):
            load_config(link)
        self.values[FIELDS[0]] = "relative.p12"
        self.write()
        with self.assertRaises(ValueError):
            load_config(self.config)


class ArtifactVerificationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.apk = Path(self.temp.name) / "fixture.apk"
        self.apk.write_bytes(b"synthetic APK")
        self.env = dict(zip(FIELDS, ("/fixture.p12", "synthetic-password", "fixture", "synthetic-password")))
        self.der = b"synthetic certificate"
        self.digest = hashlib.sha256(self.der).hexdigest()
        self.signature = "Signer #1 certificate SHA-256 digest: {}\n".format(self.digest).encode()
        self.manifest = b"package: name='com.recipegrabber' versionCode='1'\n"
        self.keytool = patch("release.java_tool", return_value="keytool")
        self.keytool.start()
        self.addCleanup(self.keytool.stop)

    def test_correct_signer_and_expected_package(self):
        with patch("release.run", side_effect=[self.der, self.signature, self.manifest]), patch("builtins.print"):
            verify(self.apk, self.env)

    def test_wrong_signer_is_rejected_before_manifest(self):
        with patch("release.run", side_effect=[self.der, b"Signer #1 certificate SHA-256 digest: aabb\n"]):
            with self.assertRaises(ValueError):
                verify(self.apk, self.env)

    def test_debuggable_and_wrong_variant_are_rejected(self):
        for manifest in (self.manifest + b"application-debuggable\n", self.manifest.replace(b"com.recipegrabber'", b"com.recipegrabber.releasecheck'")):
            with patch("release.run", side_effect=[self.der, self.signature, manifest]):
                with self.assertRaises(ValueError):
                    verify(self.apk, self.env)

    def test_unsigned_bundle_is_rejected_even_if_jarsigner_returns_zero(self):
        with patch("release.run", side_effect=[self.der, b"jar is unsigned", b"Not a signed jar file"]):
            with self.assertRaises(ValueError):
                verify_bundle(self.apk, self.env)

    def test_subprocess_failure_does_not_expose_output(self):
        result = subprocess.CompletedProcess(["fixture"], 1, b"synthetic-secret", b"synthetic-secret")
        with patch("release.subprocess.run", return_value=result):
            with self.assertRaises(RuntimeError) as failure:
                run(["fixture"], self.env)
        self.assertNotIn("synthetic-secret", str(failure.exception))


if __name__ == "__main__":
    unittest.main()
