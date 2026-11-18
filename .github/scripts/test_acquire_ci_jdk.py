# Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
# Released under AGPL v3.0 with additional terms; see LICENSE and NOTICE.
import copy
import hashlib
import io
import json
from pathlib import Path
import tarfile
import tempfile
import unittest
from unittest import mock

import acquire_ci_jdk as bootstrap


class ReviewedJdkTests(unittest.TestCase):
    def setUp(self):
        workspace = Path(__file__).resolve().parents[2]
        base = workspace / "build/tmp/ci-jdk-tests"
        base.mkdir(parents=True, exist_ok=True)
        self.owned = tempfile.TemporaryDirectory(dir=base)
        self.root = Path(self.owned.name).resolve()
        self.assertTrue(self.root.is_relative_to(base.resolve()))
        self.pin = json.loads((workspace / "gradle/ci-jdk.json").read_text())

    def tearDown(self):
        self.owned.cleanup()

    def archive(self, name, content=b"fixture", kind=tarfile.REGTYPE, link=""):
        path = self.root / "fixture.tar.gz"
        with tarfile.open(path, "w:gz") as archive:
            member = tarfile.TarInfo(name)
            member.type, member.linkname, member.mode = kind, link, 0o644
            member.size = len(content) if kind == tarfile.REGTYPE else 0
            archive.addfile(member, io.BytesIO(content) if kind == tarfile.REGTYPE else None)
        return path

    def test_current_reviewed_pin_is_accepted(self):
        bootstrap.validate_pin(self.pin)

    def test_moving_insecure_or_lookalike_sources_rejected(self):
        for source in (self.pin["source"].replace("https:", "http:"),
                       self.pin["source"].replace("corretto.aws", "corretto.aws.example.com"),
                       "https://corretto.aws/downloads/latest/amazon-corretto-21-x64-linux-jdk.tar.gz"):
            with self.subTest(source=source):
                pin = copy.deepcopy(self.pin)
                pin["source"] = source
                with self.assertRaises(ValueError):
                    bootstrap.validate_pin(pin)

    def test_dynamic_version_wrong_platform_and_hash_rejected(self):
        for key, value in (("version", "21"), ("platform", "windows-x64"),
                           ("sha256", "not-a-checksum")):
            with self.subTest(key=key):
                pin = copy.deepcopy(self.pin)
                pin[key] = value
                with self.assertRaises(ValueError):
                    bootstrap.validate_pin(pin)

    def test_checksum_mismatch_rejected(self):
        path = self.root / "artifact"
        path.write_bytes(b"reviewed")
        bootstrap.verify_checksum(path, hashlib.sha256(b"reviewed").hexdigest())
        path.write_bytes(b"changed")
        with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
            bootstrap.verify_checksum(path, hashlib.sha256(b"reviewed").hexdigest())

    def test_redirects_fail_closed(self):
        with self.assertRaisesRegex(ValueError, "redirect"):
            bootstrap.VendorRedirects().redirect_request(None, None, 302, "", {},
                                                        "https://example.com/artifact")

    def test_safe_archive_extracts_expected_bytes(self):
        path = self.archive("jdk/bin/java")
        home = bootstrap.extract_archive(path, self.root / "output")
        self.assertEqual((home / "bin/java").read_bytes(), b"fixture")

    def test_path_escapes_and_empty_root_rejected_before_writes(self):
        for name in ("../escape", "/absolute", "jdk/../escape", "C:/escape", "jdk\\escape", "."):
            with self.subTest(name=name):
                path = self.archive(name)
                with self.assertRaises(ValueError):
                    bootstrap.extract_archive(path, self.root / "output")
                self.assertFalse((self.root / "output").exists())

    def test_links_and_special_files_rejected(self):
        for kind in (tarfile.SYMTYPE, tarfile.LNKTYPE, tarfile.CHRTYPE, tarfile.FIFOTYPE):
            with self.subTest(kind=kind):
                with self.assertRaises(ValueError):
                    bootstrap.extract_archive(self.archive("jdk/link", kind=kind, link="../../escape"),
                                              self.root / "output")

    def test_archive_size_and_entry_limits(self):
        path = self.archive("jdk/file", b"large")
        for constant in ("MAX_ENTRIES", "MAX_EXPANDED"):
            with self.subTest(constant=constant), mock.patch.object(bootstrap, constant, 0):
                with self.assertRaisesRegex(ValueError, "limits"):
                    bootstrap.extract_archive(path, self.root / "output")

    def test_wrong_identity_and_incomplete_jdk_rejected(self):
        home = self.root / "jdk"
        (home / "bin").mkdir(parents=True)
        (home / "bin/java").touch()
        values = {"IMPLEMENTOR": "Amazon.com Inc.", "IMPLEMENTOR_VERSION": "Corretto-" + self.pin["version"],
                  "JAVA_RUNTIME_VERSION": self.pin["java_runtime"], "OS_NAME": "Linux", "OS_ARCH": "x86_64"}
        def release():
            (home / "release").write_text("\n".join(f'{k}="{v}"' for k, v in values.items()))
        release()
        with self.assertRaisesRegex(ValueError, "complete JDK"):
            bootstrap.verify_identity(home, self.pin)
        (home / "bin/javac").touch()
        bootstrap.verify_identity(home, self.pin)
        values["JAVA_RUNTIME_VERSION"] = "21+35-LTS"
        release()
        with self.assertRaisesRegex(ValueError, "differs from pin"):
            bootstrap.verify_identity(home, self.pin)

    def test_failed_download_identity_does_not_install(self):
        destination = self.root / "installed"
        for limit, message in ((bootstrap.MAX_DOWNLOAD, "SHA-256 mismatch"), (1, "download limit")):
            with self.subTest(limit=limit):
                opener = mock.Mock()
                opener.open.return_value = io.BytesIO(b"unreviewed artifact")
                with mock.patch.object(bootstrap, "MAX_DOWNLOAD", limit), \
                        mock.patch.object(bootstrap.urllib.request, "build_opener", return_value=opener):
                    with self.assertRaisesRegex(ValueError, message):
                        bootstrap.acquire(self.pin, destination)
                self.assertFalse(destination.exists())
                self.assertEqual(list(self.root.iterdir()), [])

    def test_existing_destination_preserved(self):
        destination = self.root / "installed"
        destination.mkdir()
        marker = destination / "keep"
        marker.write_bytes(b"owned elsewhere")
        with self.assertRaises(FileExistsError):
            bootstrap.acquire(self.pin, destination)
        self.assertEqual(marker.read_bytes(), b"owned elsewhere")


if __name__ == "__main__":
    unittest.main()
