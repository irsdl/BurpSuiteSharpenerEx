# Opt-in full Burp UI regression tests

Use this suite after major UI, settings/lifecycle, shortcut or native integration
changes. It is **not** part of `build`, `test`, `check`, normal CI, push readiness,
or the distributable jar. Existing quick headless tests remain the normal build.
The developer harness is built separately under `build/full-ui-harness/`.

## Commands

Normal validation (no live Burp/UI tests):

```text
./gradlew build --offline --dependency-verification strict
```

Compile/list the full suite and validate its catalog, report encoding and settings
isolation without starting Burp:

```text
./gradlew fullUiTestPlan --offline --dependency-verification strict
```

Run against an existing, reviewed Burp distribution and a compatible JVM. Use
`.\gradlew.bat` on Windows; supply paths from your installation:

```text
./gradlew fullUiTest -PburpJar=/path/burpsuite.jar -PburpJava=/path/to/java -PfullUiClipboard=true --offline --dependency-verification strict
```

For Community, add `-PfullUiEdition=community`. This excludes Pro licence/cache
data, selects Community in the private preferences, uses a temporary project
instead of a Pro project file, and verifies the edition reported by Montoya.
The jar is launched with `-jar` so its manifest JVM access settings are retained.
`-PfullUiEdition=professional` explicitly verifies Professional; `auto` is the
default and records the actual edition without enforcing one. JVM temporary files
also live under the disposable run directory, avoiding unrelated temp-file prompts.

For an installed Windows install4j launcher, additionally pass
`-PburpExecutable=/path/to/BurpSuite.exe`. The harness uses a guarded test-only
premain agent to add its classes to that launcher's class loader; it does not
transform Burp bytecode, edit the installation or start a debugger listener.
On macOS/Linux, use the executable desktop jar and compatible Java runtime.
An installer-only jar can behave differently from the executable desktop jar.

An active desktop is required. Professional needs an already activated installation;
Community does not use Pro activation. The Professional test seed preserves the existing local licence and opaque activation-cache
entries, unchanged and only in memory. On Windows these come from Java's
registry-backed preferences; the same public API is used on macOS/Linux.
Never activate a disposable test profile: activations are limited. A missing
cache fails preflight, and a startup guard stops the owned process if an
activation prompt appears. Ordinary project/startup prompts may be completed
manually; the suite starts only when Burp loads the developer extension.
A timeout, activation prompt or failed bootstrap
produces a **blocked/incomplete** report, never a success. The runner owns and
closes only the process it launched. The default timeout is 600 seconds; use
`-PfullUiTimeoutSeconds=900` when needed (allowed range 30–1800).

## Isolation and scope

- Each run creates a fresh project, data directory, nonce guard and reports under
  `build/reports/full-ui-tests/run-*/`. Never target an existing project or attach
  this harness to a working Burp process.
- The test JVM uses an in-memory Java preferences factory. Only existing local
  licence, activation-cache records and previously accepted startup/EULA flags are copied to that
  process over a private stdin pipe, then discarded; they are not written to test
  files or reports. Ordinary user settings, extension lists, scripts, histories
  and projects are not imported. All preference writes remain in the test JVM.
  Existing registry/preferences entries are never changed. No new activation,
  terms or authentication are performed by the test runner. An unsupported cache
  format blocks testing; it is not a reason to activate again.
- Sharpener receives real Montoya services except private test-only global/project
  preference stores and captured logging. This protects saved Sharpener settings.
  That adapter's JSON round trip is tested; real Burp persistence/save/reload is
  deliberately a separate manual case, not a claimed adapter test success.
- Repeater and Intruder receive three synthetic requests each, addressed to
  `127.0.0.1:9`. They are seeded into tools and never sent. The suite exercises
  actual Sharpener Swing action listeners, dialogs, headers and live host APIs.
  It does not send traffic to targets or automatically open browser links.
- The Proxy/Logger cases additionally send small HTTP fixtures through a
  disposable loopback-only Proxy listener to an owned loopback origin, both on
  temporary ports. The harness checks origin bytes and Content-Length, Proxy
  history annotations, and live table-renderer colours in Proxy and Logger.
  It restores the test host's listener/interception settings and closes its
  server/observer registrations. These client-side test sockets are excluded
  from the production jar; no external target receives fixture traffic.
- Clipboard tests require `-PfullUiClipboard=true`. Without it the clipboard case
  is blocked. They accept only an empty/plain-text clipboard and restore the
  original text afterward; rich clipboard formats block the case before changes.
  No original clipboard content is included in reports. Run in a test desktop
  session with no clients routed through the disposable host's proxy.
- Screenshot cases paint only the synthetic tool component and save local PNG
  fixtures. Windows icon cases can create the documented hashed ICO cache entries.
  Raw host logs remain local under the ignored build directory and may contain
  host account/startup information; do not upload them without review.

## Coverage

The [case catalog](../src/fullUiTest/resources/ui-cases.json) is the executable
inventory. The automated portion covers:

- Actual extension startup, main/tool discovery, registration and unload cleanup.
- Every main-menu theme, icon size and tool checkbox; enable/disable all;
  scrolling, geometry, capability and debug controls.
- Title and icon input/chooser dialogs, bundled/custom icons, reset confirmation,
  About, shortcut table Save/Close/Reset and parent ownership.
- Repeater and Intruder presets, fonts/sizes, icons, bold/italic/color, copy/paste
  styles, regex title operations, title history, navigation and mouse-wheel hooks.
- Clipboard and file screenshot actions, synthetic request/response context actions,
  isolated style persistence and cancel paths for destructive menu actions.
- Request-editor assertions are activated if its feature flag is enabled. The
  currently disabled request/suite/response placeholders are reported explicitly;
  the suite does not invent UI for unfinished features.
- Menu coverage links observed actionable paths to exercised scenarios or named
  manual cases. New unmatched enabled controls fail the coverage case. A visible
  control is not proof of physical mouse/keyboard usability.
- `proxy-highlighting` exercises temporary URL/body marker removal (including
  mixed case and repeated markers), permanent request/response markers, invalid
  markers, disabled handling, rewritten Content-Length and visible row colours.
  `logger-highlighting` compares the live Proxy/Logger colour palette and checks
  that other-tool HTTP markers colour Logger without changing their wire content.
  These bring the opt-in catalog to 33 case groups (25 automated, eight manual).

The [manual cases](full-ui-manual-cases.md) complete desktop interaction coverage:
browser/update handoff, native taskbar/Dock appearance, physical key capture and
conflicts, real detach/group/close/reorder, light/dark themes, monitors, real host
persistence/reload/reset-all and large-project/resource behaviour.

## Reports and completing the review

Each run emits `results.json`, `results.html`, `manual-checklist.json`, progress,
synthetic fixtures and private host logs. Results distinguish `PASS`, `FAIL`,
`BLOCKED`, `MANUAL_REQUIRED` and `NOT_APPLICABLE`. All applicable automated and
manual cases must be resolved before an overall full pass. The normal full task
returns nonzero while a required manual/blocked case remains.

For development iterations, `-PfullUiAutomatedOnly=true` allows a zero exit only
when all automated cases passed (or have a justified feature-flag N/A). It still
labels the overall report **incomplete** while manual work remains. It cannot
waive a failed assertion, skipped clipboard precondition or startup problem.

After actually completing the manual cases on this same host/jar, copy
`manual-checklist.json` to `completed-checklist.json`. Enter the real reviewer,
`tested_utc` in UTC, each result and concrete evidence. Use `NOT_APPLICABLE` only
for a genuinely unavailable feature with an explanation; do not use it to skip
testing a supported platform. Validate without starting Burp:

```text
./gradlew fullUiTestReview -PfullUiResults=/path/results.json -PfullUiManualEvidence=/path/completed-checklist.json --offline --dependency-verification strict
```

This writes `reviewed-results.json`, preserving the original automated results.
It checks host/jar identity, reviewer, dates and complete manual coverage; it
cannot establish whether an attestation is truthful. It cannot override an
automated failure or blocked case. Real human review remains necessary.

For a major release, repeat on Windows, macOS and Linux for both Stable and Early
Adopter and retain six distinct results. Link them from the release evidence;
the existing security/provenance and host checks still apply. The full suite is
explicitly invoked by the maintainer, not silently added to ordinary builds.

## Current validation limits

The latest installed-launcher Windows Early Adopter 2026.11 run recorded 23
passing case groups, zero failed assertions, one blocked rich-format clipboard
case, one disabled-editor N/A and eight required manual cases. The task correctly
returns nonzero while clipboard/manual coverage remains incomplete. Three fresh
runs reused the existing registry activation without another activation prompt.
The initial run exposed two extension defects (empty context selection and
label-based close controls) and harness caption/dialog/screenshot timing issues;
the latest run verifies their fixes. Original failed reports remain unchanged in
[the live validation record](full-ui-validation.json). No full pass or other
OS/channel compatibility is established. The expanded run verifies actual
Proxy/Logger temporary/permanent marker behaviour and matching renderer colours.

The Windows Community 2026.3.3 standalone jar also recorded 21 PASS, zero FAIL,
one clipboard BLOCKED, one disabled-editor N/A and eight manual cases pending.
Montoya reported `COMMUNITY_EDITION`, build `20260303000047134`, on Java 21.
This verifies exercised Community functionality in an older Stable release; it
does not replace current Stable or macOS/Linux checks. That result is bound to
the earlier jar/catalog. The expanded 33-group highlighter rerun timed out at
Community startup while desktop control was unavailable; no assertions ran.
Its blocked report is retained, and a new run is required.
