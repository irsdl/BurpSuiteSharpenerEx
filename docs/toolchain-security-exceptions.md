# Build-tool security exceptions

The current 2026-11-18 JDK security patch and exact Windows/Linux pins are recorded
in [the follow-up](toolchain-security-update-2026-11-18.json). That update supersedes
the local JDK selection below. This dated review retains its original findings;
the unresolved Gradle/provenance findings still block release.

Evaluation: 2026-11-16 UTC. Status: **Corretto security update applied and
tested under the user's approved age exception. Gradle's age exception is
approved, but its upgrade remains blocked by additional security findings.**
The authorization covers the current security-fix task, not publication or
unrelated dependency updates. See `toolchain-security-approval.json` for the
recorded instruction, approver context, exact artifacts, and validation.

Subsequent inspection of the complete Gradle distribution found a further
blocker: 8.14.7 bundles Guava 33.4.6-jre and Commons Lang 2.6, flagged by
GHSA-xxph-c9ww-hj94 and GHSA-j288-q9x7-2f5v. The newer 9.9.1 distribution
also bundles Guava 33.4.6-jre. See `dependency-security-review.json` for the
exact OSV query coverage/results (70 identifiable Maven inputs in 8.14.7 and
61 in 9.9.1). Guava upstream additionally lists GHSA-v5x9-67m3-7m56.
No technical non-applicability waiver or human approval of these remaining
vulnerability findings exists. The user's age approval does not waive them.
Consequently, even approval of the age exception below does **not** permit
admission of either Gradle candidate. Defer the toolchain/release until an
official corrected distribution is available or a separately evidenced and
human-approved disposition is recorded. Do not modify/repackage Gradle's libs.

## Concrete changes

1. **Proposed, still blocked:** upgrade the Gradle wrapper from 8.9 to
   **8.14.7**, staying on the same major.
   Set `distributionUrl` to
   `https://services.gradle.org/distributions/gradle-8.14.7-bin.zip` and
   `distributionSha256Sum` to
   `903bfc4bc7622323124054c10f509246212a1902948bdaf26c379cca8735e856`.
   Replace the wrapper jar with the official 8.14.7 wrapper whose SHA-256 is
   `ad16bca46cb71bb5887c546860d93992aeee35c1fdaf4a242b0a612375d937e0`.
   Regenerate wrapper launchers from the verified distribution and test offline.
2. **Applied:** use **Amazon Corretto 21.0.13.0.11
   (OpenJDK 21.0.13+11-LTS)** for local
   validation, unpacked into ignored temporary storage without changing the
   machine's global Java installation. Windows x64 artifact:
   `https://corretto.aws/downloads/resources/21.0.13.0.11/amazon-corretto-21.0.13.0.11-windows-x64-jdk.zip`,
   SHA-256 `2245721d8cec120ebddf60a6981a13baa89d82997dab621a0ee6a5849beee233`.
   Record the exact vendor/build in the build evidence. CI action upgrades are
   separately blocked by their bundled npm findings; this exception does not
   waive those findings or approve a release.

## Advisory, age, and provenance evidence

- [Gradle 8.14.7 release notes](https://docs.gradle.org/8.14.7/release-notes.html)
  identify the three new fixes: [GHSA-3cfg-jpcr-gfp3](https://github.com/gradle/gradle/security/advisories/GHSA-3cfg-jpcr-gfp3),
  [GHSA-gqrr-4j89-5267](https://github.com/gradle/gradle/security/advisories/GHSA-gqrr-4j89-5267),
  and [GHSA-pcp8-98xm-c6pr](https://github.com/gradle/gradle/security/advisories/GHSA-pcp8-98xm-c6pr).
  All affect 8.9; 8.14.7 is the narrowest public stable 8.x release fixing them.
  Earlier affected releases also have repository-fallback/deserialization
  advisories. This build resolves dependencies online and handles PR source;
  a non-applicability waiver is not being asserted.
- Gradle distribution Last-Modified: **2026-11-11 07:33:58 UTC** (5 days old).
  The official [checksum service](https://services.gradle.org/distributions/gradle-8.14.7-bin.zip.sha256)
  and [wrapper checksum](https://services.gradle.org/distributions/gradle-8.14.7-wrapper.jar.sha256)
  match independently downloaded bytes. The detached distribution signature
  verifies cryptographically with subkey
  `F3FF33E96F18AA62DD580F9651FBF517CE6D6B80`, primary key
  `EA96F38569C044AAEF7FCF732887F479B0B9771A` (Gradle Inc.).
  Key material came from the Ubuntu public keyserver; no independent web-of-trust
  certification is claimed. Gradle is Apache-2.0; bundled notices remain in its zip.
- [Corretto release record and published hashes](https://github.com/corretto/corretto-21/releases/tag/21.0.13.0.11):
  publication **2026-10-27 00:08:25 UTC**, versus changelog October 24.
  Windows artifact Last-Modified is later: **2026-10-30 01:04:05 UTC**
  (17 days old), so that later date governs age. The zip matches the release's
  SHA-256 and its official detached signature verifies with the
  [vendor-distributed key](https://yum.corretto.aws/corretto.key), fingerprint
  `6DC3636DAE534049C8B94623A122542AB04F24E3`.
  The zip's release metadata confirms vendor, build, Windows, and x86_64.
  Corretto is OpenJDK GPLv2 with Classpath Exception and bundled third-party notices.
- [Corretto changelog](https://github.com/corretto/corretto-21/blob/21.0.13.0.11/CHANGELOG.md)
  identifies [CVE-2026-114820](https://www.oracle.com/security-alerts/alert-cve-2026-114820.html)
  in java.security and the October quarterly fixes, including JSSE. This is the
  vendor's current stable Java 21 security build. JDK network/cryptographic and
  Swing APIs are used by this build and extension; no non-applicability claim
  is made. The existing local 21.0.2 predates the intervening security fixes.

## Approval and disposition

- Human security approver and follow-up owner: **repository maintainer irsdl**
  (connected GitHub account `irsdl`, local Git identity Soroush Dalili), via the
  user's instruction in this session. This is not agent self-approval.
- Authorization: "I was about to approve patching vulnerable dependencies even
  if they are under 30 days"; applied to the previously reviewed current-task
  official security fixes. Approval-record UTC is in the JSON record; it is
  the recording time, not an asserted exact chat-message timestamp.
- Regression/package validation: **266 tests passed, zero failures/errors/
  skips**, in a clean offline strict build with Corretto 21.0.13.0.11.
  `verifyPackagedJar` and the separate packaged-dependency smoke test passed,
  including actual Windows x64 JNA native memory access. Live Burp
  load/unload/reload is still required before publication. Gradle 8.14.7 was
  not executed or installed as the wrapper because other security gates fail.
- First weekly review: **2026-11-23 UTC**.
- Close Gradle exception once exact artifacts reach 31 days (no earlier than
  **2026-12-12 07:33:58 UTC**), after advisory recheck.
- Close Windows Corretto exception no earlier than **2026-11-30 01:04:05 UTC**,
  after advisory recheck.
- Expiry: **2026-12-17 UTC** (31 days after evaluation). Do not carry a vulnerable
  rollback toolchain into release; revert/defer the update/release if validation
  fails. The old artifacts are diagnostic rollback inputs only.

Approval of the age exceptions does not waive other admission gates, authorize
publication, or certify that all embedded toolchain components have been audited.
