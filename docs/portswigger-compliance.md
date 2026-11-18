# PortSwigger review and publication gates

Reviewed 2026-11-18 against the official
[AI extension best practices](https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/creating-ai-extensions/best-practices)
and [BApp Store acceptance criteria](https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/bapp-store-acceptance-criteria).
Both pages reported an update on 2026-11-17. Re-read them before each push and
release; this mapping must follow later upstream changes. Repository gates are
additional project requirements, not a claim of PortSwigger certification.

Version 4.93 is unreleased. This review retains the security/toolchain and live
host findings below; it does not establish full compliance or release readiness.
The changelog and README distinguish implemented changes from measured support.

## BApp criteria mapped to this repository

| # | Review area | Evidence and remaining work |
|---|---|---|
| 1 | Distinct purpose | Maintain the existing [Sharpener listing](https://portswigger.net/bappstore/3c5025b0e19d419a8f339ee0c30391dd), rather than submit a duplicate. Review newly added functions against the current catalogue. |
| 2 | Name and description | Preserve Sharpener's established name, explain its UI customisations in README and release notes, and keep the BApp description accurate. The listing currently describes 4.92. |
| 3 | Secure inputs | Review traffic, clipboard titles, remote version metadata, imported settings, images and themes as untrusted. No execution of message content. Existing proxy colour parsing and plain-text editor tests cover some paths; complete the final security and host review before release. |
| 4 | Bundled requirements | Runtime libraries, native members, resources and notices are packaged and checked by `verifyPackagedJar`. Montoya is host-provided. No runtime dependency installer. Remaining provenance/toolchain findings are in [dependency review](dependency-review.md). |
| 5 | Responsiveness and concurrency | Owned bounded workers handle updates, browser launching, clipboard reads and screenshot writes. Dialogs run on the EDT; delayed failures are logged. Test large-project latency, cancellation, image/theme loading and lock contention in a real host. |
| 6 | Unload lifecycle | Montoya unload handler stops worker/delayed queues, UI monitor, listeners and owned UI. Synthetic tests cover interruption, failure isolation and pane cleanup. Host/native calls may ignore interruption: demonstrate termination and no callbacks against unloaded state in live tests. |
| 7 | HTTP transport | Optional update checks use Montoya HTTP, upstream TLS verification and a response timeout. Browser links open only on explicit user action. ResourceIconCache's JarURLConnection reads local jar resources. No passive audit network calls. |
| 8 | Offline use | UI features require no internet service. Update checking defaults off and network failures do not disable core functionality. Confirm with internet unavailable in both host channels. |
| 9 | Large projects | Proxy callbacks retain no long-term HTTP message references or project-wide history snapshots. Review tab/title history growth, large messages and many tabs; measure memory/latency before release. Use Montoya temporary-file storage if long-lived message retention is ever added. |
| 10 | Window ownership | Dialog/file/colour/shortcut prompts use the suite frame; missing-frame paths cancel or suppress supported dialogs. Test ownership, light/dark themes, multiple monitors and unload with dialogs open. |
| 11 | Official API dependency | Gradle resolves `net.portswigger.burp.extensions:montoya-api`, aligned for compile/test, with locks and verification; never bundle host API classes. |
| 12 | AI provider | Not applicable today: there are no model calls or AI-powered features. Any future AI implementation must meet the following section, with Burp AI selected initially. |

Existing listing acceptance is not approval of the new jar. This source review
does not prove full security, responsiveness, or live Burp compatibility.

## Rules if AI functionality is introduced

- Default to Burp AI; declare `EnhancedCapability.AI_FEATURES` through
  `enhancedCapabilities()` and check `api.ai().isEnabled()` before use.
- Additional third-party AI providers must use Montoya networking with
  `RequestOptions.withUpstreamTLSVerification()` and the same capability checks.
- Encode model output for its display context. Treat prompts, traffic and model
  output as untrusted; never let them authorize execution or external actions.
- Minimise data, enforce target scope, remove secrets where feasible, and validate
  outgoing structured payloads. JSON formatting alone does not stop prompt injection.
- Consider private, bounded caching keyed by every generation input; do not persist
  sensitive prompts accidentally. Describe purpose/role/task and prefer low temperature.
- Handle `PromptException`, unavailable AI, exhausted credits and service failures
  without breaking other features. Keep AI calls off the EDT with owned cancellation.
- Test disabled AI, malicious output, scope/redaction, duplicate requests and unload.
  Document provider choice, disclosure and costs before enabling a paid operation.

## Before pushing

1. Read [AGENTS.md](../AGENTS.md) and both official pages again. Check current BApp
   metadata and any changed requirements, not only this historical summary.
2. Review the exact diff, all 12 areas and AI applicability; record evidence and
   unresolved items in [portswigger-review.json](portswigger-review.json).
3. Run `./gradlew portswiggerReviewDigest --offline --dependency-verification strict`.
   After the review, record that input digest and the real upstream-check UTC.
   The digest covers source, resources, build, workflow, hooks, policy, notices
   and docs. It normalises text CRLF to LF. It excludes the report itself and
   generated build evidence to avoid self-reference; review those separately.
4. Run `./gradlew verifyPushReadiness --offline --dependency-verification strict`.
   On Windows use `.\gradlew.bat`. Commit the reviewed snapshot only when authorized,
   ensure a clean tree and push only the checked-out commit. CI repeats the gate.
5. Each clone should enable `.githooks/pre-push` with
   `git config --local core.hooksPath .githooks` (preserve/integrate custom hooks).
   On Unix also run `chmod +x .githooks/pre-push` and preserve the executable bit
   when committing it.
   Git for Windows supplies the shell. Tag pushes require the release gate.

The push gate accepts explicit release blockers for review branches because
publication is disabled. It rejects a missing, stale or incomplete review and
build/test failures. It cannot establish whether an attestation is truthful;
maintainer review remains necessary. Hooks are local and bypassable; configure
the CI job as a required branch-protection check on GitHub. No GitHub repository
settings were changed by this work.

## Before a release or BApp update

Run `verifyReleaseReadiness` with the same offline/strict flags. It intentionally
fails for the current review. Complete these steps before changing its disposition:

- Resolve [dependency/toolchain findings](dependency-review.md), refresh security
  evidence under AGENTS.md, use an admitted exact toolchain, and retain build inputs,
  test results, notices, output hash, signing status and rollback material.
- Follow the [Stable/Early Adopter host checklist](burp-ui-compatibility.md) on the
  exact jar on Windows, macOS and Linux for each release channel (six host records; include
  the `os_family` values required by the gate). Follow the
  [platform icon and dynamic testing notes](platform-icons-and-testing.md). Include
  Community functionality as claimed, multi-monitor dialogs,
  offline/update failure, a large temporary project, big messages and many tabs.
  Record edition/build/OS/JRE, tester, UTC, jar hash, measured behaviour and logs.
  Include unload during pending update/clipboard work and with a dialog open;
  confirm no surviving Sharpener worker, timer, listener or retained host object.
- Reconcile README/minimum Burp version, properties, changelog, licence and BApp
  metadata with measured support. Older API availability is not established by
  compiling against a newer Montoya artifact. Narrow unsupported claims or test
  the advertised minimum before publication.
- Complete all criterion results and release evidence in the JSON. Record real
  reviewer identities and evidence paths; never manufacture approval. Required
  release evidence fields are enforced in [the Gradle gate](../gradle/portswigger-policy.gradle).
- Run `verifyReleaseReadiness` again after final changes and immediately before
  any manual GitHub/web/API publication or BApp submission. The same rule applies
  to uploading assets to an existing release. Preserve prior releases and tags.

There is deliberately no automatic publisher. A future isolated release workflow
must enforce the same gate and dependency policy before receiving write credentials.
Tests and local checklists cannot decide BApp Store admission; PortSwigger does.
