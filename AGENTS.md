# Maintaining BurpSuiteSharpenerEx

Keep the extension, tests, documentation, build configuration, licensing, and
release artifacts self-contained. These instructions apply to AI-assisted work
throughout this repository.

- Resolve paths from the repository, user input, or documented environment
  variables. Do not add machine-specific paths, private repository references,
  credentials, Burp project data, or captured traffic to tracked files or jars.
- Keep dependencies available from public canonical sources and preserve
  third-party licences and notices. Keep LICENSE, NOTICE, source headers, and
  the notices shown by the extension consistent.
- Work through pull requests into `main`. Preserve `Legacy-Extension` unless the
  user explicitly requests a change to it. Do not commit, push, merge, or publish
  merely because a local build succeeded; follow the user's authorized scope.
- Use JDK 21 and the checked-in Gradle wrapper. Run commands from this directory:
  `./gradlew build` (`.\gradlew.bat build` on Windows) runs the headless tests and
  produces `releases/BurpSuiteSharpenerEx.jar`. `./gradlew jar` packages without
  running the tests and is not sufficient release validation.
- Prepare the pinned build toolchain separately with
  `./gradlew --sharpener-acquire-toolchain` online before the first build.
  The JDK-only wrapper guard verifies the canonical inputs and every composed
  installation file; normal build/offline commands never acquire toolchain inputs.
  Builds use a single-use daemon to prevent reuse of stock Gradle's old Guava.
- Keep `version` in `src/main/resources/extension.properties` parseable as a
  number and strictly greater numerically for a new release: the update checker
  compares doubles. Add a matching section at the top of CHANGES.md using
  `## Version X.Y (YYYY-MM-DD)` for release notes.
  Keep each section brief: a few factual bullets describing user-visible changes.
  Mark unreleased work explicitly. Keep implementation/audit detail in `docs/`,
  and never imply compatibility or release approval beyond recorded evidence.

## PortSwigger checks before every push and release

Before every GitHub push (including tags) and every release/BApp submission,
re-read both live upstream documents, compare them with the repository's current
behaviour, and follow [the compliance checklist](docs/portswigger-compliance.md):

- [AI extension best practices](https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/creating-ai-extensions/best-practices)
- [BApp Store acceptance criteria](https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/bapp-store-acceptance-criteria)

Review all criteria, even when no AI code changed. AI-assisted development alone
does not make Sharpener an AI extension. Record AI applicability explicitly; if
AI features are introduced, all AI gates in the checklist become mandatory.
Do not declare AI capability or add an AI provider just to satisfy this policy.

Update `docs/portswigger-review.json` with UTC/source dates, evidence for all 12
criteria, AI applicability, unresolved findings, push/release dispositions, and
the current input digest from `portswiggerReviewDigest`. A digest is a snapshot
identifier, not proof of review: never refresh dates, statuses, or hashes without
actually reviewing the changed inputs and official guidance. Preserve truthful
pending/blocked results; never turn missing host or security evidence into a pass.
Review gates require a Git checkout. Their digest includes tracked files even
under ignored paths and non-ignored new candidates; ignored untracked local
tooling must not affect it. Keep the input-selection regression checks passing.

Before pushing, run `./gradlew verifyPushReadiness --offline
--dependency-verification strict` using the reviewed JDK/cache. This builds,
tests, checks packaging, and requires a source-bound upstream review no older
than 24 hours. Review relevant failures and the exact diff to be pushed. A branch
push can retain documented release blockers only when publication is disabled.
Keep the working tree clean and validate the exact checked-out commit being
pushed. Local hooks use `git config --local core.hooksPath .githooks`; enable this
in each clone without overwriting an existing custom hook configuration. Do not
forget `chmod +x .githooks/pre-push` on Unix; preserve that executable bit when
committing the hook. Do not
skip hooks/checks with `--no-verify` or bypass required CI checks.

Before any tag push, release/asset publication or BApp submission, run
`./gradlew verifyReleaseReadiness --offline --dependency-verification strict`.
Ordinarily all criteria and host/security evidence must be resolved. The
maintainer's explicit 2026-11-18 approval in `docs/release-4.93-approval.json`
permits GitHub 4.93 with the listed UI, OS/channel, clipboard, raw-evidence,
minimum-version and provenance findings deferred. Preserve their true results;
never turn a deferral into a pass or claim BApp certification. This is approval
to publish 4.93 now, not a new activation request or a future-version waiver.
The release gate emits the exact jar SHA-256, source revision, test results and
approval scope in a build receipt. Known applicable vulnerabilities, failed
automated tests, wrong sources/versions/hashes and missing notices remain errors.

Normal PR/main CI is read-only and must never publish. The separate manually
dispatched release workflow runs only on main: read-only Windows/Linux builds
precede an isolated publisher which rebuilds the same pinned inputs, runs the
release gate and requires the identical Windows jar hash. The write token is
exposed only to the publication step. Preserve existing releases/tags/assets;
never overwrite them. Require passing Linux `build` and `windows-build` server
checks. Passing checks alone does not approve future releases.

## Opt-in full UI regression suite

The heavy host suite lives in `src/fullUiTest/`, with Gradle tasks defined in
`gradle/full-ui-tests.gradle`. It is intentionally absent from `build`, `check`,
`test`, normal CI, push readiness and jar packaging. Never attach it to those
tasks. Use `fullUiTestPlan` to compile/list/validate the harness without launching
Burp, and `fullUiTest` explicitly after major UI, shortcut, native integration,
settings/lifecycle changes, or when a maintainer requests a full regression.
Keep raw host evidence outside `build/`: the default report directory is ignored
`temp/full-ui-test-reports/`, which survives Gradle clean builds. Back up existing
evidence before any operation that can remove its storage directory.
For Professional, reuse the installation's existing activation cache without modifying it. Never
request a new activation for a disposable test run: activations are limited.
Missing required cached activation or an activation prompt must block/stop the run.
For Community, use `-PfullUiEdition=community`, exclude Pro licence/cache data,
use a temporary project and verify the actual host edition. Do not count an older
Stable host as validation of the current Stable release or waive untested OSes.
Follow [the full-suite instructions](docs/full-ui-tests.md). Record the exact
host and jar results; a blocked startup, failed action or unfinished manual case
is not a full pass. Do not copy a result to another OS/channel. Keep the developer
harness out of the distributed extension, use disposable projects and isolated
settings, and do not download unreviewed UI automation libraries or test hosts.
Changes to user-facing menus/actions must update the test plan and menu coverage;
ordinary quick unit tests remain the default build validation.

For the maintainer-approved 4.93 release, full-host/clipboard/manual regressions
are deferred and must not block normal CI or publication. Future automation work
should replace the eight manual groups with Windows-compatible automated cases
where practical, keeping the heavy suite opt-in. Do not fabricate Community
results from Professional: their shared API objects support a compatibility
expectation, not a completed Community run. Add real other-OS tests when available.

## Windows, macOS and Linux compatibility

Keep all three platforms supported for both current Stable and Early Adopter.
Do not infer native compatibility from headless or mocked operating-system tests.
Guard Windows shell/JNA calls behind the Windows platform check. Preserve macOS
Dock behaviour and portable Swing window icons; missing desktop/taskbar/native
capabilities must leave independent features usable. Derive paths at runtime,
never modify installed executables or existing user shortcuts, and restore exact
owned window properties/listeners on reset/unload. Keep file/codec work off the EDT.
Do not add a platform-specific dependency without the existing admission review.
Record real platform evidence when available and keep untested platforms pending.
The 4.93 maintainer exception defers the six-host matrix; it does not remove
Windows/macOS/Linux code support. Prioritize current Stable/Early Adopter and
roughly the preceding year. Older hosts can use historical GitHub jars; the
technical Java 21 version floor is not a promise of full older-host compatibility.

## Dependency and CI admission

These rules apply to new and updated direct, transitive, compile-only, runtime,
build, test, vendored, and embedded/native components, the JDK, Gradle and its
wrapper/plugins, and GitHub Actions. Published binary releases require review of
the complete resolved inventory, including unchanged components. An existing
reference, version comment, passing build, or successful scanner result is not
proof of approval or benign code.

| Gate | Requirement |
|---|---|
| Upstream and identity | Use the canonical maintained vendor, foundation, or project. Verify package coordinates, publisher, public history, and security reporting route. Investigate unexplained ownership changes. |
| Distribution | Acquire Maven dependencies only from the configured Maven Central repository. Acquire Gradle, JDKs, and action sources from their canonical official distributions. No lookalikes, unofficial forks/repackaging, arbitrary VCS dependencies, JitPack, local Maven repositories, or mixed private/public feeds. A verified byte-identical cache is acceptable. |
| Necessity | Explain the need; prefer the JDK or an existing approved component. Keep the extension jar and build/test toolchain minimal. |
| Age | The stable version and exact artifact must each have been public for 31 complete 24-hour days, evaluated in UTC. Reject withdrawn or prerelease production dependencies. |
| Security | Check upstream advisories and recognized vulnerability databases, including vendored and embedded/native components. Unavailable metadata is not a clean result. Resolve applicable vulnerabilities before release. |
| Artifact | Pin identity/version, filename, classifier and applicable OS/architecture, canonical source, and SHA-256. Record Maven coordinates and hashes for both dependency metadata and executable/binary artifacts. For GitHub Actions, retain the full reviewed commit SHA and release/tag-to-commit evidence. |
| Provenance and licence | Verify available signatures/attestations; document reviewed gaps instead of inventing verification. Preserve compatible original licence and notice requirements, including those of embedded components. |
| Build | Review/acquire online, verify exact artifacts, then compile, test, and package offline with no uncontrolled downloads. Review new Gradle plugins, build scripts, annotation processors, test agents, and action code before execution. |

The existing Montoya API, Gson, Guava, Commons Lang, FlatLaf, RSyntaxTextArea,
AutoComplete, Reflections, JNA, vendored preference classes, JUnit, Mockito, and
required official build/CI tools are candidate families, not blanket approval of
their versions or transitive/embedded code. Burp's own API and libraries supplied
by Burp must be distinguished from libraries bundled in the extension jar.
Preserve settings compatibility, including the vendored preferences' JSON format.

The maintainer explicitly approved one distribution-policy exception on
2026-11-18: compose official Gradle 9.9.1 with the unmodified Maven Central
Guava 33.8.0-jre jar replacing its vulnerable bundled Guava. Exact input and
composition hashes, approval, expiry and evidence are in
`docs/gradle-security-patch.json` and `gradle/security-toolchain.properties`.
This is a project-local composition, not an official Gradle release. Preserve
the vendor archive and notices, verify the entire installation before use, and
never patch global/shared installations. No other repackaging, advisory waiver
or publication is authorized by this exception. Review weekly and retire the
composition when an admitted official distribution resolves the findings.

## Age evidence and security exceptions

`effective_publication_utc = max(version_publication_utc, artifact_publication_utc)`.
Eligibility requires `evaluation_utc >= effective_publication_utc + 31 days`.
Use authoritative Maven Central/upstream release records and exact artifact
upload or Last-Modified evidence, not cache/download times, source commit author
dates, or version comments. For actions, establish when the reviewed commit and
stable release referencing it became public. Prefer the later verified conflicting
timestamp and explain the conflict. Missing evidence blocks admission. New bytes,
reuploads, versions, or platform artifacts need review again.

The sole age exception is an official stable release fixing a known applicable
vulnerability. All other gates still apply. Record the advisory, affected/fixed
versions, applicability and reviewed fix evidence, narrowest supported fix, exact
artifacts/hashes, regression and packaged tests, named human security approver,
approval UTC, owner, follow-up, and expiry. The agent requesting an exception must
not self-approve it or infer approval from a deadline or a general update request.
Prepare the concrete evidence and proposed change before requesting that approval.

When the user authorizes younger official security fixes for the active task,
record that authorization once and proceed within its scope without asking
again for the same age exception. It does not approve unrelated upgrades,
unfixed vulnerabilities, non-applicability waivers, or publication.

Younger related dependencies need individual evidence; unrelated updates inherit
nothing. No fabricated advisory, unofficial patch, or moving branch qualifies.
Review exceptions weekly and close within 31 days through ordinary age
qualification or replacement. Do not retain an applicable vulnerable version
merely to satisfy the age gate.

## Locks, builds, and GitHub Actions

- Pin exact stable dependency/plugin versions; no dynamic versions, ranges,
  SNAPSHOTs, or changing modules. Keep main and test Montoya versions aligned.
- Commit Gradle dependency locks covering compile, runtime, and test scopes.
  Commit strict dependency verification metadata in `gradle/verification-metadata.xml`.
  Do not accept checksums generated from an unreviewed download as proof of trust.
  Record canonical artifact SHA-256 values separately in the review inventory.
- Pin the Gradle distribution and its official SHA-256 in the wrapper properties;
  verify the wrapper jar against official evidence when changing it. Pin the exact
  reviewed JDK vendor, patch/build, platform, source, and artifact hashes for
  release builds. A major-only Java version selector is not an exact toolchain pin.
- Keep a tracked review ledger at `docs/dependency-review.md` when admitting
  dependency, toolchain, or action changes. Record evaluation UTC, component/scope,
  canonical sources, version/artifact publication evidence, hashes or action SHAs,
  licences, provenance results/gaps, advisory results, decisions, and exceptions.
- Separate online acquisition/review from offline build/test/package. After all
  required artifacts have been verified, use the wrapper with `--offline` and
  strict verification. Prevent build scripts, tools, and tests from independently
  downloading executable inputs; offline dependency resolution alone is not a
  network sandbox. Local caches are not release inputs until verified.
- Do not introduce automatic dependency updates/merges, runtime installers,
  mutable tool versions, or unreviewed download hooks.
- Pin every action, including reusable/nested actions, to a reviewed full commit
  SHA with version and age evidence. Review executable transitive action inputs.
  Use read-only build/test job permissions and disable persisted checkout
  credentials. Isolate release jobs and grant only their required permissions.
  Never execute untrusted PR code with release secrets or write permissions.
- Use a fixed hosted-runner OS family/version label and record the actual image
  version. Hosted images remain managed and mutable; do not claim the label is an
  immutable pin. Pin reviewed digests when using OCI images.
- Distributable jars require passing build, test, and admission gates. Inspect the
  packaged jar, required resources, extension version, bundled dependencies,
  LICENSE/NOTICE, and applicable native libraries before upload. Exercise loading,
  unloading, and reloading in supported Burp versions, including the newest
  supported release; mocked/headless tests do not prove host compatibility.
- Retain source revision, lock/verification digests, component inventory,
  JDK/Gradle/runner identity, output SHA-256, applicable signing status, original
  notices, test results, and rollback inputs. Release security evidence must be
  current before publication. Quarantined diagnostic artifacts must not be
  presented as approved releases.

## Tests, review cadence, and current baseline

Behavior, dependency, toolchain, and build/packaging changes need appropriate
automated tests with meaningful failure cases. Documentation-only changes need
content/link validation. Fix failures; do not suppress tests or weaken assertions
to ship. Record what was actually tested, including any missing live Burp tests.

Check upstream advisories and recognized databases such as OSV, GitHub Advisory
Database, and NVD for the resolved inventory and reviewed toolchains. Record review
source and UTC. Non-applicability requires technical evidence and human security
approval. If no usable fix exists, remove, replace, or defer the affected
feature/release. Release security evidence must be no older than seven days, with
a same-day check for urgent fixes. Review deployed dependencies weekly, dependency
currency monthly, and relevant security fixes immediately. Scheduling remains a
maintainer responsibility unless recurring automation is explicitly requested.

The 2026-11-16 dependency review establishes dependency locks, strict SHA-256
verification metadata, a review ledger, a wrapper distribution checksum, and
packaged-resource/native/notice checks. See `docs/dependency-review.md` for exact
coverage, evidence, and unresolved security/provenance findings. CI now has a
fixed runner family/version label, read-only permissions, disabled persisted
checkout credentials and separate acquisition/offline validation. Publication
and release/tag deletion have been removed. The 2026-11-18 security follow-up pins
the Linux CI JDK in `gradle/ci-jdk.json` and removes marketplace Actions from the
workflow, using native Git and a reviewed standard-library JDK acquisition script.
The runner image remains managed; record its actual identity and tool versions.
The approved Gradle/Guava composition fixes the ten identified Gradle/Guava
advisories; native/build provenance, inventory gaps and host findings still block
full release admission.
Do not claim the complete toolchain is audited. The 4.93 maintainer release
approval accepts the recorded provenance/inventory gaps as non-blocking; do not
silently use it to admit unrelated inputs or waive an applicable vulnerability.
The current local validation JDK is pinned in `gradle/validation-jdk.json`;
its approved security-fix age exception is recorded in
`docs/toolchain-security-update-2026-11-18.json`, referencing the existing age-only
authorization in `docs/toolchain-security-approval.json`. Neither JDK pin waives
other findings; only the exact separately approved composition above is allowed.
Existing local diagnostic builds may be used to investigate and test fixes;
complete and document the required gates before admitting updates or publishing
binary releases. Do not silently expand a source-fix request into dependency or
workflow upgrades, or fabricate approval/evidence for existing components.
