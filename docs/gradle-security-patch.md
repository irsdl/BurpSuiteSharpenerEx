# Gradle security composition

The maintainer approved this exact local composition on 2026-11-18 after the
Windows diagnostic passed 329 tests. [The record](gradle-security-patch.json)
contains canonical sources, exact input and composition hashes, signature and
age evidence, advisory coverage, authorization and remaining limitations.

Official Gradle 9.9.1 fixes the eight identified Gradle core advisories affecting 8.9 but still
bundles vulnerable Guava 33.4.6-jre. This project replaces that one jar with the
unmodified Maven Central Guava 33.8.0-jre artifact, fixing both recorded Guava
advisories. Every other distribution file stays byte-identical. This is a
maintainer-approved local composition, **not an official Gradle distribution**.
The exception permits neither other repackaging nor publication.

## Building

Use JDK 21 and the checked-in wrapper. On Windows substitute `.\gradlew.bat`.
Prepare the toolchain and locked project inputs online, then build offline:

```sh
./gradlew --sharpener-acquire-toolchain
./gradlew resolveDependencies --dependency-verification strict
./gradlew build --offline --dependency-verification strict
```

Toolchain acquisition is an exclusive command: combining it with build tasks
or `--offline` fails. Normal commands never download missing toolchain inputs.
No additional interpreter or external Gradle plugin is needed by this bootstrap.

The JDK-only guard in `gradle/bootstrap/ReviewedGradle.java`:

- Verifies the original vendor distribution, official wrapper jar, fixed Guava
  jar and POM against the reviewed pins before use. Downloads accept only the
  canonical endpoints and Gradle's restricted official delivery chain.
- Assembles into `.gradle/security-toolchain/gradle-9.9.1-guava-33.8.0-jre/`.
  The original vendor archive stays intact; no global/shared install is patched.
  Downloads and extraction are bounded, staged and checked before publication.
- Preserves original licences/notices. The replacement jar retains its original
  Maven identity and licence. Its installation filename remains
  `guava-33.4.6-jre.jar` solely for Gradle's internal classpath descriptors;
  its actual metadata and bytes are **33.8.0-jre**. Do not infer its version from
  that compatibility alias or scan the archived vendor zip as an active install.
- Compares every installed file against the canonical zip plus the one fixed
  jar on every invocation; rejects extra files, links and changed content.
  A sorted path/NUL/SHA-256/newline manifest has a pinned platform-independent
  composition digest. Ownership and executable-mode metadata are not part of it.
- Delegates to the verified distribution's public Gradle launcher, using JDK
  class loading rather than private wrapper methods removed by vendor shrinking.
  The checked-in launchers are intentionally adapted; the official wrapper jar
  remains unmodified and separately verified. Regenerating launchers with the
  upstream `wrapper` task would discard this security guard and needs review.
- Forces a fresh single-use daemon; `--daemon` is rejected. `check` also verifies
  the actual daemon's Guava code-source SHA-256. A stock Gradle daemon therefore
  cannot silently supply the old library.

Corrupt inputs/installations fail closed and are never silently repaired. Keep
evidence before removing the specifically named project cache and reacquiring.
The guard is build-only and is excluded from the distributed extension.

## Validation and retirement

The updated Windows wrapper passes 346 normal tests, packaging, daemon identity
checks and the 33-group opt-in UI harness plan. Windows Git Bash also starts the
Unix launcher. These are build checks, **not macOS/Linux Burp UI validation**.
Fresh OSV queries found no matches for 61 identifiable composed Gradle Maven
coordinates. Unidentified/shaded/native inventory and publisher-provenance gaps
remain documented; no universal vulnerability-free claim is made.

[Linux CI passed](https://github.com/irsdl/BurpSuiteSharpenerEx/actions/runs/44318611310)
with the identical composition digest and fixed daemon Guava bytes. Exact
commit, runner/JDK identities and raw-log hash are in the JSON record.
Review the exception weekly,
starting 2026-11-23, and retire it when a fully reviewed official distribution
fixes the embedded findings. The existing security-fix age authorization expires
2026-12-17; this record does not extend it. All host/release gates remain required.
