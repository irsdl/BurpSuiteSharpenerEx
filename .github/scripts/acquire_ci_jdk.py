#!/usr/bin/env python3
# Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
# Released under AGPL v3.0 with additional terms; see LICENSE and NOTICE.
"""Acquire the reviewed CI JDK. No third-party Python packages or moving URLs."""
import argparse
import hashlib
import json
import os
import platform
from pathlib import Path, PurePosixPath
import re
import sys
import tarfile
import tempfile
import urllib.request
import zipfile

MAX_DOWNLOAD = 512 * 1024 * 1024
MAX_EXPANDED = 1024 * 1024 * 1024
MAX_ENTRIES = 10000


def validate_pin(pin):
    version = pin.get("version", "")
    if not re.fullmatch(r"21\.\d+\.\d+\.\d+\.\d+", version):
        raise ValueError("Expected an exact reviewed Corretto 21 build")
    suffix = {"linux-x64": "linux-x64.tar.gz", "windows-x64": "windows-x64-jdk.zip"}.get(pin.get("platform"))
    filename = f"amazon-corretto-{version}-{suffix}"
    expected_url = f"https://corretto.aws/downloads/resources/{version}/{filename}"
    if (pin.get("vendor") != "Amazon Corretto" or suffix is None
            or pin.get("filename") != filename or pin.get("source") != expected_url
            or not re.fullmatch(r"[0-9a-f]{64}", pin.get("sha256", ""))
            or not re.fullmatch(r"21\.[0-9.+-]+LTS", pin.get("java_runtime", ""))):
        raise ValueError("Pin must identify the exact canonical vendor artifact and SHA-256")


class VendorRedirects(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Versioned vendor resources currently do not redirect. Reject any
        # change rather than following an unreviewed distribution location.
        raise ValueError("Unexpected redirect for pinned vendor artifact")


def verify_checksum(path, expected):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    if digest.hexdigest() != expected:
        raise ValueError("JDK artifact SHA-256 mismatch")


def extract_archive(archive_path, output):
    if archive_path.suffix == ".zip":
        return extract_zip(archive_path, output)
    output = output.resolve()
    with tarfile.open(archive_path, "r:gz") as archive:
        members = []
        total = 0
        roots = set()
        for member in archive:
            path = PurePosixPath(member.name)
            if (not member.name or not path.parts or path.is_absolute() or ".." in path.parts
                    or "\\" in member.name or re.match(r"^[A-Za-z]:", member.name)
                    or not (member.isdir() or member.isfile())):
                raise ValueError("Unsafe or unsupported JDK archive entry")
            # The reviewed vendor archive has no links. Rejecting links also
            # prevents link-based path escapes during subsequent extraction.
            if not (output / member.name).resolve().is_relative_to(output):
                raise ValueError("JDK archive path escapes staging directory")
            roots.add(path.parts[0])
            total += member.size
            members.append(member)
            if len(members) > MAX_ENTRIES or total > MAX_EXPANDED:
                raise ValueError("JDK archive exceeds reviewed extraction limits")
        if len(roots) != 1:
            raise ValueError("Expected a single JDK root directory")
        # Entries, sizes and paths are checked before writing any archive bytes.
        # Ownership metadata is deliberately not restored.
        for member in members:
            target = output / member.name
            if member.isdir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.extractfile(member) as source, target.open("xb") as dest:
                    while block := source.read(1024 * 1024):
                        dest.write(block)
                target.chmod(member.mode & 0o755)
        return output / next(iter(roots))


def extract_zip(archive_path, output):
    output = output.resolve()
    with zipfile.ZipFile(archive_path) as archive:
        members = archive.infolist()
        roots, names, total = set(), set(), 0
        for member in members:
            path = PurePosixPath(member.filename)
            mode = member.external_attr >> 16
            if (not path.parts or path.is_absolute() or "\\" in member.orig_filename
                    or any(p in (".", "..") or ":" in p for p in member.filename.split("/"))
                    or "//" in member.filename or mode & 0o170000 == 0o120000
                    or member.filename.lower() in names
                    or not (output / member.filename).resolve().is_relative_to(output)):
                raise ValueError("Unsafe or duplicate JDK zip entry")
            names.add(member.filename.lower())
            roots.add(path.parts[0])
            total += member.file_size
        if len(roots) != 1 or len(members) > MAX_ENTRIES or total > MAX_EXPANDED:
            raise ValueError("JDK zip exceeds reviewed roots/limits")
        for member in members:
            target = output / member.filename
            if member.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(member) as source, target.open("xb") as dest:
                    while block := source.read(1024 * 1024):
                        dest.write(block)
        return output / next(iter(roots))


def verify_identity(home, pin):
    release = home / "release"
    if not release.is_file() or release.stat().st_size > 65536:
        raise ValueError("Missing or oversized JDK release identity")
    values = {}
    for line in release.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            values[key] = value.strip('"')
    expected = {"IMPLEMENTOR": "Amazon.com Inc.",
                "IMPLEMENTOR_VERSION": "Corretto-" + pin["version"],
                "JAVA_RUNTIME_VERSION": pin["java_runtime"],
                "OS_NAME": "Windows" if pin["platform"] == "windows-x64" else "Linux", "OS_ARCH": "x86_64"}
    if any(values.get(key) != value for key, value in expected.items()):
        raise ValueError("Extracted JDK vendor/build/platform differs from pin")
    suffix = ".exe" if pin["platform"] == "windows-x64" else ""
    if not (home / ("bin/java" + suffix)).is_file() or not (home / ("bin/javac" + suffix)).is_file():
        raise ValueError("Archive is not a complete JDK")


def acquire(pin, destination):
    validate_pin(pin)
    destination = destination.absolute()
    parent = destination.parent.resolve()
    if destination.exists() or destination.is_symlink():
        raise FileExistsError("Refusing to replace an existing JDK destination")
    parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="sharpener-jdk-", dir=parent) as owned:
        staging = Path(owned).resolve()
        if not staging.is_relative_to(parent):
            raise ValueError("Staging directory escapes the selected destination parent")
        artifact = staging / pin["filename"]
        opener = urllib.request.build_opener(VendorRedirects())
        with opener.open(pin["source"], timeout=60) as response, artifact.open("xb") as out:
            size = 0
            while block := response.read(1024 * 1024):
                size += len(block)
                if size > MAX_DOWNLOAD:
                    raise ValueError("JDK artifact exceeds download limit")
                out.write(block)
        verify_checksum(artifact, pin["sha256"])
        home = extract_archive(artifact, staging / "unpacked")
        verify_identity(home, pin)
        os.rename(home, destination)
    return destination.resolve()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--destination", type=Path, required=True)
    args = parser.parse_args()
    if sys.platform not in ("linux", "win32") or platform.machine().lower() not in ("x86_64", "amd64"):
        parser.error("This bootstrap requires a reviewed Linux/Windows x64 runner")
    pin_path = Path(__file__).resolve().parents[2] / ("gradle/validation-jdk.json" if sys.platform == "win32" else "gradle/ci-jdk.json")
    home = acquire(json.loads(pin_path.read_text(encoding="utf-8")), args.destination)
    if any(char in str(home) for char in "\r\n"):
        raise ValueError("JDK destination cannot contain environment-file delimiters")
    with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as stream:
        stream.write(f"JAVA_HOME={home}\n")
    with open(os.environ["GITHUB_PATH"], "a", encoding="utf-8") as stream:
        stream.write(f"{home / 'bin'}\n")
    print(f"Verified JDK: {pin_path.name}; {home}")


if __name__ == "__main__":
    main()
