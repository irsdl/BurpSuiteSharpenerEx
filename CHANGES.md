# Change History

Release notes for the Sharpener Burp Suite extension. The newest version is at the top.

The CI release job copies the section of the released version into the GitHub release notes.
Keep the heading format exactly as `## Version X.Y (YYYY-MM-DD)` so it can find the section.

## Version 4.8 (2026-07-14)

- New Repeater and Intruder tabs that were missed by the tab change listener are now detected when the user clicks on them. Sharpener schedules a delayed reload for that tool, so the tab no longer needs a drag and drop or another tab change to be recognised.
- A missed tab is now also detected when the total tab count has not changed, for example when one tab was closed and a new one was added while an update was already pending.
- The PwnFox Highlighter now removes the `X-PwnFox-Color` header just before the request is sent, so other extensions can read the header first (issue #24).
- A new option, "PwnFox: Remove the color header" under Global Settings > Supported Capabilities, can keep the header in the outgoing request for tools that need it. Removal stays on by default.

## Version 4.7 (2026-07-14)

- The custom Burp Suite icon now also changes the Windows taskbar icon when Burp is started from its native launcher. Before this fix, it only worked when Burp was started with `java -jar`.
- The custom Burp Suite icon now changes the macOS Dock icon as well.
- Custom icons are applied in multiple sizes (16 to 128 pixels) for sharper rendering in taskbars, window switchers, and title bars. The original icon is fully restored on reset or unload.
- Burp version detection now uses the Montoya `buildNumber()` value because the older version methods are deprecated and return misleading values on current Burp releases.
- The minimum supported Burp version is now 2024.2, the first release that requires Java 21.
- Sub-tab styling has been updated for the tab bar changes in Burp 2026.
- Groundwork for a future message editor has been added but stays disabled by default: undo and redo, request loading, and quick copy and paste experiments.
- A new regression test suite (over 100 headless tests) covers styles, colors, icons, version parsing, and helper classes.
- GitHub Actions workflows have been hardened: all actions are pinned to commit SHAs and release steps only run for branch pushes.
- The `early-adopter` branch has been retired. All development now happens through pull requests into `main`.

## Version 4.5 and older

Older versions were released before this file existed.
See the [commit history](https://github.com/irsdl/BurpSuiteSharpenerEx/commits/main/) and the [releases page](https://github.com/irsdl/BurpSuiteSharpenerEx/releases).
