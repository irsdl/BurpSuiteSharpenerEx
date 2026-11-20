# Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
# Released under AGPL v3.0 with additional terms; see LICENSE and NOTICE.
import datetime
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest import mock
import zipfile
import publish_release as publisher


class ReleaseGuardTests(unittest.TestCase):
    def setUp(self):
        self.owned = tempfile.TemporaryDirectory()
        self.root = Path(self.owned.name)
        for directory in ("docs", "build/reports", "releases", "src/main/resources"):
            (self.root / directory).mkdir(parents=True)
        self.properties = self.root / "src/main/resources/extension.properties"
        self.properties.write_text("version=4.93\n", encoding="utf-8")
        self.revision = "a" * 40
        self.env = {"GITHUB_REPOSITORY": publisher.REPOSITORY, "GITHUB_REF": "refs/heads/main",
                    "GITHUB_EVENT_NAME": "workflow_dispatch", "GITHUB_SHA": self.revision,
                    "GITHUB_WORKFLOW_REF": publisher.REPOSITORY + "/.github/workflows/release.yml@refs/heads/main"}
        now = datetime.datetime.now(datetime.timezone.utc)
        self.approval = {"schema_version": 1, "approved": True, "version": "4.93", "tag": "main_4.93",
                         "repository": publisher.REPOSITORY, "human_approver": "fixture-reviewer",
                         "instruction": "fixture approval", "approval_recorded_utc": (now - datetime.timedelta(minutes=5)).isoformat(),
                         "expires_utc": (now + datetime.timedelta(days=1)).isoformat(),
                         "deferred_criteria": [3], "deferred_finding_ids": ["fixture-gap"]}
        self.review = {"release_disposition": "approved-with-maintainer-exception", "release_blockers": [],
                       "reviewed_input_sha256": "b" * 64, "upstream_checked_utc": now.isoformat(),
                       "criteria": [{"id": i, "reviewed": True, "evidence": "fixture evidence",
                                     "result": "pending" if i == 3 else "satisfied"} for i in range(1, 13)],
                       "ai_review": {"result": "not-applicable"}, "deferred_findings": [{"id": "fixture-gap", "status": "untested"}]}
        self.jar = self.root / "releases/BurpSuiteSharpenerEx.jar"
        self.make_jar()
        self.env["EXPECTED_JAR_SHA256"] = publisher.sha256(self.jar)
        self.approval["deferred_findings"] = self.review["deferred_findings"]
        self.write("docs/release-4.93-approval.json", self.approval)
        self.write("docs/portswigger-review.json", self.review)
        self.receipt = {"source_revision": self.revision, "version": "4.93", "tag": "main_4.93",
                        "repository": publisher.REPOSITORY, "maintainer": "fixture-reviewer",
                        "approval_record_sha256": publisher.sha256(self.root / "docs/release-4.93-approval.json"),
                        "reviewed_input_sha256": "b" * 64, "deferred_findings": self.review["deferred_findings"],
                        "jar_sha256": publisher.sha256(self.jar), "jar_bytes": self.jar.stat().st_size,
                        "tests": {"tests": 1, "failures": 0, "errors": 0}}
        self.write("build/reports/release-evidence.json", self.receipt)
        (self.root / "CHANGES.md").write_text("## Version 4.93 (2026-11-18)\n\n- Fixture notes.\n")

    def tearDown(self):
        self.owned.cleanup()

    def write(self, name, data):
        (self.root / name).write_text(json.dumps(data), encoding="utf-8")

    def make_jar(self, version="4.93", notices=True):
        with zipfile.ZipFile(self.jar, "w") as archive:
            archive.writestr("extension.properties", "version=" + version + "\n")
            if notices:
                archive.writestr("META-INF/LICENSE", "fixture licence")
                archive.writestr("META-INF/NOTICE", "fixture notice")

    def check(self, clean=True):
        return publisher.validate_inputs(self.root, self.env, self.revision, clean)

    def test_approved_exact_artifact_is_accepted_with_gaps_preserved(self):
        jar, receipt, notes = self.check()
        self.assertEqual(jar, self.jar)
        self.assertEqual(receipt["deferred_findings"], self.review["deferred_findings"])
        self.assertIn("Fixture notes", notes)

    def test_previous_approval_cannot_authorize_next_version(self):
        self.properties.write_text("version=4.94\n", encoding="utf-8")
        with self.assertRaises(FileNotFoundError): self.check()
        self.write("docs/release-4.94-approval.json", self.approval)
        with self.assertRaises(ValueError): self.check()

    def test_new_version_requires_its_own_approval_artifact_and_receipt(self):
        self.properties.write_text("version=4.94\n", encoding="utf-8")
        approval = dict(self.approval, version="4.94", tag="main_4.94")
        self.write("docs/release-4.94-approval.json", approval)
        self.make_jar("4.94")
        self.env["EXPECTED_JAR_SHA256"] = publisher.sha256(self.jar)
        self.receipt.update(version="4.94", tag="main_4.94", jar_sha256=publisher.sha256(self.jar),
                            jar_bytes=self.jar.stat().st_size,
                            approval_record_sha256=publisher.sha256(self.root / "docs/release-4.94-approval.json"))
        self.write("build/reports/release-evidence.json", self.receipt)
        (self.root / "CHANGES.md").write_text("## Version 4.94 (2026-11-20)\n\n- Fixture notes.\n")
        self.assertEqual(self.check()[0], self.jar)

    def test_malformed_or_duplicate_source_versions_rejected(self):
        for value in ("version=../4.93\n", "version=4.93\nversion=4.94\n",
                      "version=4.93\nversion=NaN\n", "version=NaN\n"):
            self.properties.write_text(value, encoding="utf-8")
            with self.subTest(value=value), self.assertRaises(ValueError): self.check()

    def test_wrong_event_ref_workflow_or_source_rejected(self):
        for key, value in (("GITHUB_EVENT_NAME", "pull_request"), ("GITHUB_REF", "refs/heads/feature"),
                           ("GITHUB_WORKFLOW_REF", "untrusted workflow"), ("GITHUB_SHA", "c" * 40)):
            with self.subTest(key=key), mock.patch.dict(self.env, {key: value}):
                with self.assertRaises(ValueError): self.check()
        with self.assertRaises(ValueError): self.check(clean=False)

    def test_missing_tested_hash_and_changed_jar_rejected(self):
        with mock.patch.dict(self.env, {"EXPECTED_JAR_SHA256": ""}):
            with self.assertRaises(ValueError): self.check()
        self.jar.write_bytes(self.jar.read_bytes() + b"changed")
        with self.assertRaises(ValueError): self.check()

    def test_expired_wrong_version_or_absent_approval_rejected(self):
        for key, value in (("approved", False), ("version", "4.94"), ("human_approver", ""),
                           ("expires_utc", "2020-01-01T00:00:00Z")):
            data = dict(self.approval, **{key: value})
            self.write("docs/release-4.93-approval.json", data)
            with self.subTest(key=key), self.assertRaises(ValueError): self.check()

    def test_stale_review_and_unapproved_findings_rejected(self):
        for key, value in (("upstream_checked_utc", "2020-01-01T00:00:00Z"),
                           ("release_blockers", ["new security defect"]), ("ai_review", {"result": "pending"})):
            self.write("docs/portswigger-review.json", dict(self.review, **{key: value}))
            with self.subTest(key=key), self.assertRaises(ValueError): self.check()

    def test_changed_receipt_and_failed_tests_rejected(self):
        for key, value in (("source_revision", "d" * 40), ("approval_record_sha256", "0" * 64),
                           ("deferred_findings", []), ("tests", {"tests": 1, "failures": 1, "errors": 0})):
            self.write("build/reports/release-evidence.json", dict(self.receipt, **{key: value}))
            with self.subTest(key=key), self.assertRaises(ValueError): self.check()

    def test_unreleased_notes_rejected(self):
        (self.root / "CHANGES.md").write_text("## Version 4.93 (2026-11-18)\n\nUnreleased.\n")
        with self.assertRaises(ValueError): self.check()

    def test_wrong_packaged_version_or_missing_notices_rejected(self):
        for version, notices in (("4.94", True), ("4.93", False)):
            self.make_jar(version, notices)
            self.env["EXPECTED_JAR_SHA256"] = publisher.sha256(self.jar)
            self.receipt["jar_sha256"] = publisher.sha256(self.jar)
            self.receipt["jar_bytes"] = self.jar.stat().st_size
            self.write("build/reports/release-evidence.json", self.receipt)
            with self.subTest(version=version, notices=notices), self.assertRaises(ValueError): self.check()

    def test_invalid_context_cannot_call_github(self):
        with mock.patch.dict(os.environ, {"GITHUB_EVENT_NAME": "pull_request"}), \
                mock.patch.object(publisher, "git", side_effect=[self.revision, ""]), \
                mock.patch.object(publisher, "api") as api:
            with self.assertRaises(ValueError): publisher.publish(self.root, {"GITHUB_EVENT_NAME": "pull_request"})
            api.assert_not_called()


if __name__ == "__main__":
    unittest.main()
