# Change History

## Version 4.93 (2026-11-16)

Unreleased. Full UI validation and release approval remain pending.

- Fixed loading, main-tab discovery, tab-icon and empty context-menu failures with Burp's changed UI.
- Added Logger marker highlighting and safer UI recovery that preserves saved styles and Burp's listeners.
- Added an ICO resource for Windows launcher icons, with restoration on reset/unload.
- Improved background work, update checks and screenshot handling.
- Updated dependencies, removed unused libraries, and added dependency verification and third-party notices.
- Added opt-in full UI tests and PortSwigger push/release checks; automatic publishing is disabled.

## Version 4.92 (2026-07-21)

- Vendored the preference classes, preserving saved settings and removing the JitPack dependency.

## Version 4.91 (2026-07-21)

- Enabled TLS certificate verification for the optional update check.
- Moved clipboard reads off the UI thread and avoided redundant native icon updates.
- Stopped the disabled placeholder request editor from adding demo headers.
- Detected and safely unloaded duplicate live copies.
- Added source copyright notices and AGPL section 7 terms in NOTICE.

## Version 4.9 (2026-07-15)

- Improved off-screen window recovery and saved window bounds.
- Added configurable shortcuts, editor/tab focus navigation and tab-selection history.
- Fixed accidental title pasting, tab focus, menu scrolling and main-tab icon clicks.
- Restored native tab styling, key bindings and listeners on unload.
- Cached icons, removed Spring, shortened UI startup waits and added a technical reference.

## Version 4.8 (2026-07-14)

- Detected missed Repeater/Intruder tabs, including replacements with unchanged tab counts.
- Delayed PwnFox colour-header removal until request sending and added an option to retain it.

## Version 4.7 (2026-07-14)

- Added Windows taskbar/macOS Dock icon handling and multiple icon sizes.
- Switched version detection to Montoya's build number and set the Java 21 baseline to Burp 2024.2.
- Updated sub-tab styling for Burp 2026 and added headless regression tests.
- Added disabled message-editor groundwork and moved development to pull requests into main.

## Version 4.5 and older

See the [commit history](https://github.com/irsdl/BurpSuiteSharpenerEx/commits/main/) and [releases](https://github.com/irsdl/BurpSuiteSharpenerEx/releases).
