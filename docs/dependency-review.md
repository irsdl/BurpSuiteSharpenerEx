# Dependency review: 2026-11-16

## GitHub 4.93 publication approval: 2026-11-18

The maintainer explicitly accepted the remaining provenance/inventory and host
evidence gaps for GitHub 4.93 in [the release approval](release-4.93-approval.json).
They remain incomplete, not passed; they do not block this authorized release.
Known applicable vulnerabilities, failed automated tests, wrong sources/hashes
and missing notices remain errors. No Maven dependency or marketplace Action is
added by this release change. The existing signed/hash-pinned Windows JDK input
now serves Windows CI as well as local validation; standard-library zip guards
and the manual publisher have 23 passing positive/negative tests. Hosted runner Git/Python/
GitHub CLI binaries remain managed inputs within the accepted provenance gap.
The original dated findings below are historical; use the release approval for
the current 4.93 publication disposition, not their former blocking language.

## Approved Gradle security composition: 2026-11-18

The maintainer separately approved replacing Gradle's bundled Guava after
reviewing the tested proposal. The checked-in wrapper now uses official Gradle
**9.9.1** plus the unmodified official **Guava 33.8.0-jre** jar, composed only in
the project's private cache. This resolves eight identified Gradle core
advisories and both Guava advisories; no non-applicability waiver was used.
Exact inputs, checksums, signatures, age evidence, licence, scope, expiry and
remaining inventory/provenance limits are in [the approval record](gradle-security-patch.json).

The JDK-only guard performs explicit online acquisition separately, preserves
the canonical archive/notices, and compares every installed file before each
launch. Its stable composition digest is pinned. A fresh single-use daemon and
a daemon-side Guava hash assertion prevent reuse of an old stock installation.
The 17 new regression cases cover corrupted/missing input, bounded downloads,
redirects, archive traversal/collisions/limits, changed/extra installed files,
wrong Guava metadata, incorrect pins and preservation of existing data/notices.
No helper or build dependency is included in the extension jar.

Offline strict build: **346 tests passed**, packaging and the 33-group harness
plan passed; no new Burp host run was performed. Sixty-one identifiable composed
Gradle Maven coordinates returned no matching OSV advisories. This is dated
coverage, not proof of a complete shaded/native inventory or release admission.
The earlier 36-coordinate runtime/test review remains unchanged. Linux CI and
remaining provenance/host evidence are recorded separately, without inference.

[PR #57 Linux CI passed](https://github.com/irsdl/BurpSuiteSharpenerEx/actions/runs/44318611310),
logging the same composition digest and fixed daemon Guava bytes on the exact
tested merge commit. JDK/runner identities and raw-log hash are in the record;
this is build evidence, not a Burp host or macOS validation.

The dated reviews below retain the pre-patch findings and decisions.

## Security patch follow-up: 2026-11-18

- Upgraded the local Windows validation JDK and pinned Linux CI to **Amazon
  Corretto 21.0.13.1.2 / OpenJDK 21.0.13.1+2-LTS**. The official stable release
  fixes CVE-2026-114938 through CVE-2026-114946. Both exact artifacts match vendor
  SHA-256 values and detached signatures. The new bytes were published on
  2026-11-18; the existing current-task age-only authorization covers this
  security fix. Exact review, timestamps, hashes, licence, expiry and nine
  published CNA records are in [the update record](toolchain-security-update-2026-11-18.json).
- Removed `actions/checkout` and `actions/setup-java` from the active workflow.
  Reviewed newer releases still contain flagged npm dependencies; no vendor
  action was repackaged or labelled clean. Native Git fetches/verifies the exact
  tested public commit without stored credentials. The checked-in Python
  standard-library bootstrap verifies the pinned vendor archive before extraction
  and checks its vendor/build/platform. Twelve positive/negative tests cover
  wrong sources/hashes/identities, redirects, path escapes, links, limits and
  preservation of existing destinations. CI keeps read-only permissions and
  publication disabled; the managed runner image is not an immutable tool pin.
  A fresh query of both removed baseline lockfiles found 5 and 7 flagged declared
  runtime package/version entries; source SHAs, metadata hashes and query coverage
  are retained in the security JSON. This is not a claim that their minified
  bundles were independently reproduced. [PR #56 CI passed](https://github.com/irsdl/BurpSuiteSharpenerEx/actions/runs/44307321081)
  and logged the exact patched runtime, native Git/Python versions and runner image.
- Refreshed OSV checks for **36 inventoried Maven coordinates**, including parent
  metadata: no matching advisories returned. Runtime/test versions and dependency
  locks are unchanged. This is dated query coverage, not a guarantee of security.
- **Gradle remains blocked.** The latest official releases are still 8.14.7
  and 9.9.1. Rehashed their canonical archives and rescanned their embedded Maven
  inventories: 8.14.7 still contains flagged Commons Lang 2.6 and Guava
  33.4.6-jre; 9.9.1 still contains the flagged Guava. Upstream's additional
  Guava advisory also applies. No clean official replacement was found, so the
  wrapper remains diagnostic 8.9 rather than admitting another flagged toolchain
  or unofficially modifying vendor libraries. Remaining security/provenance and
  real-host findings continue to block publication.
- A clean local offline strict build with the patched JDK passes **329 normal
  tests**, packaging and the opt-in harness plan. The production jar remains
  byte-identical to the prior diagnostic jar. Raw historical UI reports were
  accidentally removed by that clean; their committed summaries/hashes remain,
  but raw host evidence must be rerun. Future full-suite output is retained in
  ignored `temp/full-ui-test-reports/`, outside the cleanable build directory.

The historical reviews below retain their original dates, findings and decisions.

## Fresh CI metadata follow-up: 2026-11-18

The first PR build stopped at strict verification because a fresh cache fetched
`org.junit:junit-bom:5.14.4`'s Gradle module metadata, while local validation had
only needed its already reviewed POM. Reviewed the canonical Central `.module`,
published SHA-256 and signature before adding its hash. The module has been public
for 205 complete days (2026-04-26T11:20:53Z); its two sets of 18 constraints match
the unchanged reviewed POM. It contains no code, artifact files or scripts.
The signature validates with the existing JUnit fingerprint
`FF6E2C001948C5F2F38B0CC385911F425EC61B51`; identity/trust limitations remain.
OSV returned no advisories for this coordinate/version at the follow-up UTC in
[the security record](dependency-security-review.json). A fresh-cache acquisition
also required `junit-bom:6.1.3`'s POM: its canonical checksum and existing-key
signature pass, its 16 constraints match the already reviewed module, and its
exact publication date (2026-08-07T12:27:00Z) qualifies at 102 complete days.
It has no build, repositories, profiles, parent or executable content; its EPL-2.0
licence and canonical SCM match the existing component. OSV returned no advisories.
The inventory now contains 63 artifact/metadata records. Versions, locks and runtime jar contents are unchanged;
verification remains strict. Existing release blockers are retained.

PR [#55](https://github.com/irsdl/BurpSuiteSharpenerEx/pull/55) contains the review
branch. This follow-up does not approve the diagnostic CI JDK or other unresolved
toolchain/provenance findings. The original dated review below records the state
before the authorized branch push.

This is a dated local dependency-update review, **not approval to publish a
binary release**. Library changes pass the checks described below. Build/CI
security and provenance gaps remain unresolved. The user approved current-task
age exceptions for official vulnerability fixes; the Corretto fix is applied
and tested. Unfixed vulnerabilities and non-applicability waivers are not
approved. See [the approval record](toolchain-security-approval.json).

Evaluation UTC: 2026-11-16T14:44:34.326561+00:00. Base source revision: `3812640c38315ec0233d2cd1d8fcb8b90fe68236`, plus the
uncommitted 4.93 source/build changes. Owner: repository maintainer. No commit,
push, tag, release, or recurring automation was created.

## Decisions

Select the newest stable direct versions whose exact Central artifacts satisfy
31 complete days, keeping main/test Montoya and all JUnit versions aligned.
The JSON inventory records canonical filenames, coordinates, publication
headers, SHA-256, published checksum evidence, and signature results. Do not
reuse this snapshot as a current security review after seven days.

| Component | Previous | Selected | Scope | Effective publication UTC (latest selected artifact/metadata) |
|---|---|---|---|---|
| Montoya API | 2026.4 | 2026.7 | compile-only / test | 2026-07-14T13:15:53+00:00 |
| Gson | 2.11.0 | 2.14.0 | runtime | 2026-04-23T19:01:47+00:00 |
| Commons Lang | 3.20.0 | 3.21.0 | runtime | 2026-09-25T20:01:06+00:00 |
| FlatLaf | 3.7.1 | 3.7.2 | runtime / embedded natives | 2026-07-09T09:56:04+00:00 |
| RSyntaxTextArea | 3.6.3 | 4.0.1 | runtime | 2026-07-26T21:26:57+00:00 |
| JNA | 5.15.0 | 5.19.1 | runtime / embedded natives | 2026-06-12T16:48:57+00:00 |
| JUnit Jupiter | 6.1.0 | 6.1.3 | test | 2026-08-07T12:27:00+00:00 |
| JUnit launcher | 6.1.0 | 6.1.3 | test | 2026-08-07T12:27:00+00:00 |
| Mockito | 5.23.0 | 5.24.0 | test | 2026-09-23T14:06:34+00:00 |
| Reflections | 0.10.2 | 0.10.2 | runtime, unchanged | 2021-10-25T14:23:23+00:00 |

- Remove **Guava** rather than ship an unnecessary vulnerable library. Its sole
  use was `Files.getNameWithoutExtension` for icon names; JDK `File.getName`,
  `lastIndexOf` and `substring` preserve that behaviour. No other Guava imports
  or reflective references remain in source.
- Remove **AutoComplete**: source/resources contain no references to its API.
  The actively used RSyntaxTextArea dependency stays explicit.
- Keep Reflections 0.10.2: Central lists no newer stable release. It discovers
  capability settings. Its age, limited maintenance activity, and unverifiable
  signing key are limitations, not evidence of ongoing support. No unofficial
  fork was introduced. Replacement deserves a separate compatibility change.
- Gson serializes saved preferences. Exact golden JSON and reload tests passed;
  expected JSON strings were not changed. The editor major update remains
  compatible with this project's Java 21 baseline in the headless tests.
- JNA uses core APIs only. Burp may supply its own JNA through its parent loader;
  changing Sharpener's bundled jar does not upgrade the host's copy. Actual
  host/native UI behaviour still needs live validation.
- Retain upstream-selected compatible transitive versions unless the review
  finds an applicable advisory. Do not force unrelated major upgrades merely
  to increase a version number. All selected transitive scopes are locked.

## Younger versions deferred

| Component | Latest observed | Exact jar Last-Modified UTC | Decision |
|---|---|---|---|
| com.google.code.gson:gson | 2.15.0 | 2026-10-24T01:23:38+00:00 | Under 31 days; no applicable security-fix exception requested |
| org.apache.commons:commons-lang3 | 3.22.0 | 2026-11-08T14:10:59+00:00 | Under 31 days; no applicable security-fix exception requested |
| com.formdev:flatlaf | 3.8 | 2026-11-04T12:10:48+00:00 | Under 31 days; no applicable security-fix exception requested |

Guava 33.7.2-jre is old enough and fixes GHSA-xxph-c9ww-hj94, but upstream's
newer GHSA-v5x9-67m3-7m56 also affects it. The fix 33.8.0-jre was published
2026-11-02 (under 31 days). Removing the unused library avoids both advisories
and an unnecessary age exception. JUnit 6.2.0-M1 is a prerelease, not eligible.

## Identity, distribution, provenance, and age

Only `mavenCentral()` is configured. POM/Gradle module metadata and canonical
upstream project/release/security pages establish the project families; no
private feeds, lookalike coordinates, changed publishers, repository declarations
from dependency POMs, VCS dependencies, or external Gradle plugins were added.
The following purposes justify retained inputs:

- PortSwigger: the host extension API, compile-only plus mocked tests.
- Google Gson: preference JSON. Google Error Prone/JSpecify: runtime/test annotations selected
  by the official dependency metadata; not annotation processors.
- Apache Commons: existing string utilities; FormDev and Fifesoft: existing
  Swing tabs and syntax editor; JNA: Windows AppUserModelID integration.
- Reflections with Javassist, SLF4J and JSR-305: existing capability discovery.
- JUnit, OpenTest4J, API Guardian, Mockito, Byte Buddy/agent and Objenesis:
  regression assertions and mocked Burp API. The test agent is an executable
  input; it was verified before tests were run.

All Maven bytes were independently retrieved from
`https://repo.maven.apache.org/maven2/` and compared to the proposed Gradle
verification values. Every artifact matched a separately published Central
checksum (SHA-256 when published, otherwise SHA-1), and its own SHA-256 is retained
in [dependency-artifacts.json](dependency-artifacts.json). SHA-1 sidecars alone
are not treated as strong signature proof. Checksum bootstrapping from the
unreviewed local cache alone was not accepted.

For age, Central's immutable version POM/module Last-Modified is the version
publication evidence; the exact jar/POM/module's Last-Modified is artifact
publication evidence. The later timestamp governs eligibility. Current Central
metadata lists the selected stable releases. This is Central publication
metadata, not a claim to know a source commit's public visibility. All selected
Maven artifacts/metadata exceed 31 days; the youngest are Commons Lang 3.21.0
(51 complete days). No Maven age exception is used.

Detached PGP signatures verify cryptographically for **58 of 60** signed
artifacts. Reflections' jar and POM cannot be verified with the available key
material. `oss-parent:7` publishes no detached signature. Public-key material
was obtained from Ubuntu/OpenPGP public key services in an isolated keyring;
keyserver availability and a valid signature do not independently establish
publisher identity or a web of trust. Fingerprints and gaps are recorded per
artifact; no stronger provenance or reproducible upstream build is claimed.
Reflections and the unsigned parent are unchanged baseline provenance gaps,
not newly approved replacements. Strict SHA-256 verification remains enforced.

## Security review and coverage

[dependency-security-review.json](dependency-security-review.json) records all
26 resolved Maven coordinates (including the JUnit BOM), the exact OSV batch
queries/results, canonical upstream GitHub advisory responses for direct,
transitive, vendored and native-library families, and blocked tool/action
candidates. OSV returned no matching advisories for the selected Maven inventory
at evaluation time. That result is not a guarantee of safety: upstream Guava's
new advisory was absent from the OSV version query, which is why upstream
security records were checked independently.

Upstream JUnit's published advisory concerns `junit-platform-reporting`, which
is absent from the resolved inventory. Objenesis' advisory identifies 3.5;
Mockito selects 3.3, outside the stated affected version. No claim of
non-applicability to an affected retained Maven version, and no security waiver,
was used to admit the library updates.

Sources and reporting routes include the canonical GitHub `/security` pages
and upstream channels linked there; Google uses its vulnerability intake,
Apache its security process, PortSwigger its extension API repository. Several
small/legacy projects have no published security policy or advisories. Absence
of a policy/advisory is recorded coverage, not proof that no vulnerability exists.

### Embedded/native and vendored code

- [dependency-native-artifacts.json](dependency-native-artifacts.json) lists
  SHA-256 for every native member of the selected jars: 34 runtime binaries
  from JNA/FlatLaf and two test-only Byte Buddy attachment DLLs. Runtime native
  members are checked byte-for-byte against verified upstream jars during
  `verifyPackagedJar`. All OS/architecture members are retained as shipped.
- JNA's tagged `native/libffi/configure.ac` identifies libffi 3.4.4; its
  [source](https://github.com/java-native-access/jna/blob/5.19.1/native/libffi/configure.ac)
  and libffi upstream security records were reviewed. The MIT-style libffi
  notice is preserved separately. The outer jar's signature/hash and individual
  member hashes do not establish a reproducible per-platform native build or a
  complete signed native SBOM. No such attestation was available/verified.
- FlatLaf's tagged native source and Apache licence accompany its verified
  distribution. Only Windows x64 JNA memory loading was exercised locally;
  macOS, Linux, other architectures, and Windows COM/window integration were
  not exercised by that smoke test.
- Vendored preference sources were not updated. Their upstream reference,
  modifications, licence and local source hashes are in
  [dependency-vendored-sources.json](dependency-vendored-sources.json). Their
  original Corey Arthur notices and Sharpener's separate notices remain intact.
- This is not a complete independent audit of every shaded test-tool component
  or native/toolchain implementation. The unresolved provenance/build/CI
  findings below prevent presenting this as full release admission.

## Licences and packaging

[third-party-notices.md](third-party-notices.md) distinguishes bundled libraries,
Burp-provided API and test-only inputs. Missing jar notices were sourced from
matching canonical upstream source/tag records and the original JSR-305 Google
Code archive; [dependency-notice-sources.json](dependency-notice-sources.json)
records exact files, sources and hashes. JSR-305's canonical POM says Apache-2.0
while the original RI licence says BSD-3-Clause: both records are retained.
Javassist and JNA use their declared Apache-2.0 option; no licence text or
original attribution was rewritten.

The fat jar now preserves dependency notices under separate component paths,
including Commons Lang's previously excluded `.txt` notices. It retains the
extension's own root LICENSE/NOTICE, enables FlatLaf's multi-release classes,
omits dependency module descriptors, and normalizes jar order/timestamps.
These packaging settings alone do not make every upstream binary reproducible.

## Build and CI: blocked/deferred changes

- The wrapper remains **Gradle 8.9**. Its official distribution SHA-256 is now
  pinned, and the existing wrapper jar matches official SHA-256
  `498495120a03b9a6ab5d155f5de3c8f0d986a449153702fb80fc80e134484f17`.
  Source: [official 8.9 distribution checksum](https://services.gradle.org/distributions/gradle-8.9-bin.zip.sha256)
  and [wrapper checksum](https://services.gradle.org/distributions/gradle-8.9-wrapper.jar.sha256).
  A checksum validates identity, not the absence of known vulnerabilities.
- 8.9 is affected by the recent Gradle advisories. The narrowest public stable
  8.x fix is **8.14.7**, published November 11, under 31 days. Its official zip,
  wrapper hash and distribution signature were verified, but inspecting its
  bundled Maven inputs also found vulnerable Guava 33.4.6-jre and Commons Lang
  2.6. The latest **9.9.1** still bundles the flagged Guava. Neither candidate
  is admitted; an age exception alone cannot waive those security findings.
- Initial diagnostic builds used the already-installed Corretto 21.0.2. After
  the user's current-task age approval, validation now uses **Corretto
  21.0.13.0.11 / OpenJDK 21.0.13+11-LTS**, whose exact Windows artifact is
  under 31 days. Its published SHA-256 and vendor signature were verified;
  the zip was rehashed before extraction into ignored temporary storage.
  Global Java configuration was not changed. The exact local validation pin
  is [validation-jdk.json](../gradle/validation-jdk.json); approval, owner and
  expiry are in [toolchain-security-exceptions.md](toolchain-security-exceptions.md).
- Initial GitHub Actions candidates: checkout **v7.0.1**, setup-java **v6.1.0**,
  upload-artifact **v7.0.2**, and download-artifact **v8.0.0** if release-job
  separation is added. Exact canonical release-to-commit mappings are recorded
  in the security JSON. All pass age on their release dates, and GitHub reports
  verified commit signatures, but their runtime npm lock inventories include
  unresolved findings (Undici, minimatch, XML parsing and others). No action was
  upgraded or introduced. A subsequent latest-release check, now including
  younger security fixes under the user's age authorization, examined
  setup-java **v6.3.0** and download-artifact **v8.0.2** as well. Setup-java
  v6.3.0 still selects minimatch 10.2.6, affected by GHSA-g3hv-75wg-m249
  (fixed in minimatch 11.0.1); the other latest actions also retain flagged
  packages. These are unresolved security findings, not age-only blockers.
  Exact mappings and advisory detail are in `latest_actions_recheck` in the
  security JSON. Existing workflow SHAs do not imply approval either.
- Subsequent PortSwigger policy work removed automatic publication, tag/release
  deletion and artifact upload. CI now uses ubuntu-24.04, read-only permissions,
  no persisted checkout credentials, recorded runner identity and separate
  acquisition/offline validation with a source-bound review gate. Existing
  checkout/setup-java SHAs and a major-only Oracle JDK selector remain diagnostic
  baselines, not fully admitted release inputs. No new actions or versions were
  introduced. No cloud workflow or release was run. See
  [current compliance findings](portswigger-compliance.md); publication stays blocked.

## Repeating a verified local build

Use JDK 21 and the checked-in wrapper. Acquire already-reviewed locked inputs
online, then build/test/package offline with strict verification:

```sh
./gradlew resolveDependencies --dependency-verification strict
./gradlew clean build --offline --dependency-verification strict
```

Use `gradlew.bat` on Windows. Neither command updates locks or approved hashes.
A diagnostic build with the old toolchain is not an approved release build.
`--offline` stops Gradle dependency acquisition; it is not an OS network sandbox.
No new plugin, annotation processor, installer, or test download hook was added.
Changing an artifact requires a new canonical review before regenerating locks
and verification metadata, never automatic trust of locally generated hashes.

## Validation and remaining release work

- Clean offline strict build with **Corretto 21.0.13.0.11**: **299 tests, zero
  failures/errors/skips**. This
  includes exact saved-settings JSON, reload, editor undo/redo, menu startup
  checks, and existing Windows helper regressions. The subsequent UI compatibility
  hardening adds changed/ambiguous layout discovery, independent API failure,
  listener ownership, pane recovery and style preservation regressions; see
  [Burp UI compatibility](burp-ui-compatibility.md). No dependencies changed in
  that source-only follow-up.
- A live read-only probe reproduced a main-tab discovery regression in Burp build
  20261100000055090: a valid 15-tab main pane at depth 2 was discarded when an
  unrelated sidebar exceeded depth 16. The patched jar returns the correct pane
  on that same live tree; three regressions cover sibling order, nearest-layer
  ambiguity and tool-pane sidebars. This does not constitute a full extension
  load/unload/reload test. See [discovery evidence](burp-ui-discovery-evidence.json).
- PortSwigger compliance hardening adds worker interruption/failure isolation,
  delayed timer survival after an exception, timeout/TLS checks and missing-parent
  dialog cancellation. `verifyPushReadiness` passed. Manual negative gate checks
  rejected changed inputs, a stale upstream review, and a missing criterion;
  `verifyReleaseReadiness` rejected the current unresolved release findings.
  The enabled local pre-push hook also rejected this uncommitted working tree.
- Windows launcher icon hardening uses the existing JNA and JDK image codecs;
  no dependencies changed. Native shell/ICO/reset validation ran in the installed
  EXE's main window; JDI attached/detached through loopback successfully. Portable
  tests cover ICO resources and guarded Windows/macOS/Linux branches. Full visual
  and other-platform host tests remain pending; see
  [platform validation](platform-icons-and-testing.md).
- The optional full UI harness introduces no dependency/configuration versions:
  it compiles separately against the packaged jar and existing compile classpath.
  `fullUiTestPlan` passes catalog/report/private-preferences self-checks; negative
  review tests reject stale plans, jar mismatch, missing reviewers/manual cases
  and waiving blocked automation. The normal build graph excludes full UI tasks,
  and packaging rejects the harness/catalog. Initial live trials were blocked at
  startup. The 2026-11-17 follow-up completed Windows Early Adopter runs: the latest
  has 23 passing case groups, no failed assertions, clipboard blocked, one N/A and
  eight manual cases pending. Existing registry activation is reused; disposable
  runs must never request another activation. See [live evidence](full-ui-validation.json).
  See [full UI testing](full-ui-tests.md) before an explicit major-change run.
- A deliberately wrong Gson SHA-256 was rejected by strict verification offline;
  the correct verification metadata was restored. No gate/assertion was weakened.
- `verifyPackagedJar` passes: required properties/themes/notices present; host API,
  test libraries and removed Guava absent; all bundled native members identical.
- A separate headless JVM with only the built extension jar passed JNA Windows
  native memory read/write, FlatLaf setup, XML editor/theme loading, Java 9+
  multi-release class selection, and absent API/test/removed-library checks.
- Extension properties in the jar identify **4.93**. Follow-up source fixes pass
  329 normal tests. Windows Early Adopter startup/cleanup now have live evidence;
  real persistence/reload and other hosts remain pending. This does not resolve
  the dependency/toolchain release findings above.
- Source/lock/verification/output hashes and test counts are recorded in
  [dependency-build-evidence.json](dependency-build-evidence.json). The local jar
  is a diagnostic artifact; do not publish it as a fully approved release.

Before publication: resolve toolchain/CI advisories and review gaps, complete the
entire tool/native/shaded inventory and current security review, approve any
necessary exceptions, isolate CI permissions, use the exact reviewed release
JDK, record actual hosted image identity, and validate live supported Burp hosts.
First security follow-up: **2026-11-23 UTC**; currency follow-up:
**2026-12-16 UTC**. Security evidence must be refreshed within seven days and
urgent fixes checked on publication day. No schedule was created.

Rollback: retain the base revision and original dependency declarations in git
history plus these inventory/lock hashes. Reverting returns old dependencies and
is diagnostic rollback only, not a statement that the old toolchain is safe to
release. Keep the existing preserved stashes and unmerged branches intact.

## 2026-11-20 release 4.94 security recheck

No dependency, lock, verification, JDK or composed Gradle input changed for 4.94.
Requeried the same 99 recorded runtime/test/metadata and identifiable Gradle Maven
coordinates against OSV: zero advisory matches. The exact query, response and
input hashes are retained in [the recheck](release-4.94-security-check.json).
Existing canonical upstream advisory/fix and exact JDK reviews are two days old;
the complete native/shaded inventory and publisher provenance remain incomplete.
This is neither a new component admission nor a full toolchain security audit.
The version-specific publisher changes use existing standard-library facilities;
26 Python acquisition/publication guard tests pass. Publication still requires
current 4.94 acceptance of the recorded evidence gaps and passing release checks.
