#!/usr/bin/env python3
# Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
# Released under AGPL v3.0 with additional terms; see LICENSE and NOTICE.
"""Publish a version-specifically approved GitHub-built jar; preserve existing assets."""
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

REPOSITORY = "irsdl/BurpSuiteSharpenerEx"

def release_identity(root):
    properties = (root / "src/main/resources/extension.properties").read_text(encoding="utf-8")
    versions = re.findall(r"(?m)^version=([^\r\n]*)\r?$", properties)
    if len(versions) != 1 or not re.fullmatch(r"[0-9]+\.[0-9]+", versions[0]):
        raise ValueError("Release requires one numeric extension version")
    version = versions[0]
    return version, "main_" + version, root / ("docs/release-" + version + "-approval.json")


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def git(root, *arguments):
    env = dict(os.environ)
    env.pop("GH_TOKEN", None)
    return subprocess.check_output(["git", "-C", str(root), *arguments], env=env, text=True).strip()


def validate_inputs(root, env, revision, clean, now=None):
    now = now or datetime.datetime.now(datetime.timezone.utc)
    if (env.get("GITHUB_REPOSITORY") != REPOSITORY or env.get("GITHUB_REF") != "refs/heads/main"
            or env.get("GITHUB_EVENT_NAME") != "workflow_dispatch"
            or env.get("GITHUB_WORKFLOW_REF") != REPOSITORY + "/.github/workflows/release.yml@refs/heads/main"
            or not re.fullmatch(r"[0-9a-f]{40}", revision)
            or env.get("GITHUB_SHA") != revision or not clean):
        raise ValueError("Publication requires the clean, exact approved main workflow commit")
    expected = env.get("EXPECTED_JAR_SHA256", "")
    if not re.fullmatch(r"[0-9a-f]{64}", expected):
        raise ValueError("Missing tested read-only build hash")
    version, tag, approval_path = release_identity(root)
    approval = load(approval_path)
    if (approval.get("schema_version") != 1 or approval.get("approved") is not True or approval.get("version") != version
            or approval.get("tag") != tag or approval.get("repository") != REPOSITORY
            or not approval.get("human_approver") or not approval.get("instruction")
            or datetime.datetime.fromisoformat(approval["approval_recorded_utc"].replace("Z", "+00:00")) > now
            or datetime.datetime.fromisoformat(approval["expires_utc"].replace("Z", "+00:00")) <= now):
        raise ValueError("Invalid or expired release approval")
    review = load(root / "docs/portswigger-review.json")
    receipt = load(root / "build/reports/release-evidence.json")
    jar = root / "releases/BurpSuiteSharpenerEx.jar"
    if (review.get("release_disposition") != "approved-with-maintainer-exception" or review.get("release_blockers")
            or receipt.get("source_revision") != revision or receipt.get("version") != version
            or receipt.get("tag") != tag or receipt.get("repository") != REPOSITORY
            or receipt.get("maintainer") != approval["human_approver"]
            or receipt.get("approval_record_sha256") != sha256(approval_path)
            or receipt.get("reviewed_input_sha256") != review.get("reviewed_input_sha256")
            or receipt.get("deferred_findings") != review.get("deferred_findings")
            or receipt.get("jar_sha256") != expected or sha256(jar) != expected
            or receipt.get("jar_bytes") != jar.stat().st_size):
        raise ValueError("Release receipt, approval, source or tested artifact mismatch")
    checked = datetime.datetime.fromisoformat(review["upstream_checked_utc"].replace("Z", "+00:00"))
    if not datetime.timedelta(0) <= now - checked <= datetime.timedelta(hours=24):
        raise ValueError("Upstream release review must be current")
    criteria = review.get("criteria", [])
    if (sorted(c.get("id", -1) for c in criteria) != list(range(1, 13))
            or any(c.get("reviewed") is not True or not c.get("evidence") for c in criteria)
            or any(c["id"] not in approval["deferred_criteria"] for c in criteria if c.get("result") in ("pending", "blocked"))
            or any(f["id"] not in approval["deferred_finding_ids"] for f in review.get("deferred_findings", []))
            or review.get("deferred_findings") != approval.get("deferred_findings")
            or review.get("ai_review", {}).get("result") != "not-applicable"):
        raise ValueError("Unreviewed or unapproved release findings")
    tests = receipt.get("tests", {})
    if not tests.get("tests") or tests.get("failures") != 0 or tests.get("errors") != 0:
        raise ValueError("Passing automated release tests are required")
    with zipfile.ZipFile(jar) as archive:
        properties = archive.read("extension.properties").decode("utf-8")
        if not re.search(r"(?m)^version=" + re.escape(version) + r"\s*$", properties):
            raise ValueError("Wrong packaged release version")
        if not {"META-INF/LICENSE", "META-INF/NOTICE"}.issubset(archive.namelist()):
            raise ValueError("Missing packaged notices")
    changes = (root / "CHANGES.md").read_text(encoding="utf-8")
    match = re.search(r"(?ms)^## Version " + re.escape(version) + r" \(\d{4}-\d{2}-\d{2}\)\s*\n(.*?)(?=^## Version |\Z)", changes)
    if not match or re.search(r"\bunreleased\b", match.group(1), re.I):
        raise ValueError("Missing final " + version + " release notes")
    return jar, receipt, match.group(1).strip()


def gh(*arguments, data=None, allow_missing=False):
    command = ["gh", *arguments]
    if data is not None:
        command += ["--input", "-"]
    result = subprocess.run(command, input=None if data is None else json.dumps(data), text=True, capture_output=True)
    if result.returncode:
        if allow_missing and "HTTP 404" in result.stderr:
            return None
        # Do not echo commands, tokens or signed download URLs.
        raise RuntimeError("GitHub operation failed: " + result.stderr.strip())
    return json.loads(result.stdout) if result.stdout.strip() else None


def api(path, data=None, allow_missing=False):
    return gh("api", "--method", "GET" if data is None else "POST", "repos/" + REPOSITORY + path,
              data=data, allow_missing=allow_missing)


def publish(root, env):
    revision = git(root, "rev-parse", "HEAD")
    jar, receipt, notes = validate_inputs(root, env, revision, not git(root, "status", "--porcelain"))
    version, tag, _ = release_identity(root)
    current = api("/branches/main")["commit"]["sha"]
    if current != revision:
        raise ValueError("Main changed after release dispatch; rebuild/review the new main")
    run_url = f"https://github.com/{REPOSITORY}/actions/runs/{env['GITHUB_RUN_ID']}"
    receipt["workflow_url"] = run_url
    receipt["tested_build_sha256"] = env["EXPECTED_JAR_SHA256"]
    receipt["signing"] = "Unsigned jar; SHA-256 and build/review evidence supplied"
    receipt_path = root / "build/reports/release-evidence.json"
    receipt_path.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    checksum = root / "build/reports/BurpSuiteSharpenerEx.jar.sha256"
    checksum.write_text(receipt["jar_sha256"] + "  BurpSuiteSharpenerEx.jar\n", encoding="ascii")
    notes += ("\n\nBuilt and tested on GitHub Actions. Full live UI, cross-platform and provenance checks were deferred by the maintainer for " + version + "; see the attached release evidence. Community shares relevant APIs with Professional, but its current full regression was not completed. This is not BApp Store certification.\n\nBuild: " + run_url)
    existing = api("/releases/tags/" + tag, allow_missing=True)
    if existing is not None and (not existing.get("draft") or existing.get("target_commitish") != revision):
        raise ValueError("Release already exists or targets another commit; never overwrite it")
    if existing is None:
        existing = api("/releases", {"tag_name": tag, "target_commitish": revision,
                        "name": "Release " + tag, "body": notes, "draft": True, "prerelease": False})
    for asset in (jar, checksum, receipt_path):
        metadata = api("/releases/" + str(existing["id"]))
        found = next((a for a in metadata["assets"] if a["name"] == asset.name), None)
        if found is None:
            # No --clobber: preserve every existing asset and release.
            subprocess.run(["gh", "release", "upload", tag, str(asset), "--repo", REPOSITORY], check=True)
            metadata = api("/releases/" + str(existing["id"]))
            found = next(a for a in metadata["assets"] if a["name"] == asset.name)
        if found.get("digest") != "sha256:" + sha256(asset) or found["size"] != asset.stat().st_size:
            raise ValueError("GitHub asset digest/size differs; leave draft unpublished")
    gh("api", "--method", "PATCH", "repos/" + REPOSITORY + "/releases/" + str(existing["id"]),
       data={"draft": False, "make_latest": "true"})
    if api("/releases/latest")["tag_name"] != tag:
        raise ValueError("GitHub latest release did not update")
    print("Published verified latest release: https://github.com/" + REPOSITORY + "/releases/tag/" + tag)


if __name__ == "__main__":
    publish(Path(__file__).resolve().parents[2], dict(os.environ))
