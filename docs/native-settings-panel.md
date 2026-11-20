# Sharpener menu and optional native Settings (unreleased)

Initial migration prepared 2026-11-19; usability correction 2026-11-20. Development version 4.94 remains unreleased; earlier host evidence records the tested 4.93 jars. No new
dependency, toolchain, preference store or version floor is introduced. Existing
JSON serialization and registration/defaults remain in the settings classes and
vendored preferences library. The separate [manual highlighter performance work](manual-highlighter-performance.md)
adds one global inspection preference to both configuration surfaces.

## Settings and actions inventory

All values apply immediately. Opening/refreshing the panel only reads values.
“Reset all” uses the existing `Preferences.resetAll()` defaults and then unloads;
it does not delete arbitrary user data. Controls use explicit selected values.

| Control | Key / type / scope | Default | Validation, effect and reset |
|---|---|---|---|
| Enable/disable all; each tool style | `isUnique_<MainTabs>` / boolean / global | false | Existing main-tab style set/unset; all includes the same enum values as the old menu. Hidden obsolete tool entries remain hidden. |
| Theme | `ToolsThemeName` / string / global | `@irsdl` | Same eight choices; “none” stores empty string. Reset styles through existing background handler. |
| Custom theme directory | `ToolsThemeCustomPath` / string / global | empty | Accepted directory sets theme `custom`; cancellation preserves both keys. |
| Icon size | `ToolsIconSize` / string / global | `16` | Same ten sizes: 48, 32, 24, 20, 16, 14, 12, 10, 8, 6; reload styles. |
| Scrollable tool pane | `isToolTabPaneScrollable` / boolean / global | false | Set scroll/wrap policy on the EDT when a tool pane is available. |
| Remember window geometry | `useLastScreenPositionAndSize` / boolean / global | false | Existing frame listeners save/apply geometry; stored `lastApplicationPosition` (Point) and `lastApplicationSize` (Dimension) default null. |
| Off-screen recovery | `detectOffScreenPosition` / boolean / global | true | Existing window listener performs recovery. |
| Manual Highlighter (Proxy and Logger) > Enabled | `manualHighlightCapability` / boolean / global | true | HTTP/WebSocket handlers read the same capability state. Disabling preserves the saved inspection limit. |
| Manual Highlighter (Proxy and Logger) > Limit Logger inspection to 64 KiB | `manualHighlightLimitLoggerInspection` / boolean / global | true | Grouped under its feature in the menu and indented below its enable checkbox in Settings. Content-Length is only an initial hint; admitted requests are scanned completely only after actual total size is checked against 65,536 bytes. Oversized/unknown-size requests use headers; responses use their initial bytes. Incorrect declarations can still incur a full copy before rejection. Immediate effect; reset-all restores true. Proxy rewriting and WebSocket semantics remain unchanged. |
| PwnFox Highlighter | `pwnFoxSupportCapability` / boolean / global | true | Existing Proxy handler reads the same state. |
| PwnFox header removal | `pwnFoxRemoveColorHeader` / boolean / global | true | Existing handler removes/retains `X-PwnFox-Color`. |
| Debug level | `debugLevel` / integer / global | 0 (Disabled) | Same `DebugLevels` enum; update the runtime debug level and preference. |
| Update opt-in | `checkForUpdate` / boolean / global | false | User enabling checks now through the existing background/Burp networking path. Rendering, showing and refreshing never check. Startup retains the existing opt-in check. |
| Project title | `BurpTitle` / string / project | empty | Reject null/blank/unchanged input; preserve text; apply through existing asynchronous title behavior. Confirmed Reset restores the original title and saves empty. |
| Built-in window icon | `BurpResourceIconName` / string / project | empty | Same cached resource icons; save `/icons/<file>`, clear custom path; decode/apply through existing worker and native guards. |
| Custom window icon | `BurpIconCustomPath` / string / project | empty | Same image chooser/filter; cancelled/empty input leaves preferences intact. `LastBurpIconCustomPath` is registered global (empty), as before; the legacy call's PROJECT argument never changes the registered scope. |
| Reset icon | Existing project icon keys | — | Preserve existing behavior: restore owned window/native properties and clear custom path. The saved built-in resource choice and last custom path are retained. This migration does not silently alter that behavior. |
| Keyboard Shortcuts | `customShortcutsV2` / map of action names to string arrays / global | null | Existing editor, reserved-key rejection, conflict confirmation, Save/Close/Reset and immediate binding reload. Canonical key comes from `ShortcutMappings.CUSTOM_SHORTCUTS_SETTING_NAME`. Fixed header/mouse keys remain informational. |
| Unload; Remove All Settings & Unload | no new keys | — | Top menu only; existing confirmations. Only this instance's resources are disposed. |
| Project page; issue reporting; About | no keys | — | Top menu only. Browser actions remain explicit and background; About retains version, copyright, AGPL/additional-terms notice, attribution and project link. |

Sub-tab styles, context-menu actions and shortcuts continue to use the existing
project style maps and global shortcut backend. They are not a second Settings
store. The keyboard editor retains its Save/Close editing semantics; the settings
panel itself introduces no Apply/Cancel transaction.

The Manual Highlighter menu groups **Enabled** and **Limit Logger inspection to
64 KiB** in one submenu. The optional form labels the feature **Enable manual
highlighting in Proxy and Logger**, with its inspection option indented beneath
it. See the [grouping validation](manual-highlighter-menu-validation.json).

## Optional API and lifecycle

The full Sharpener top menu is registered first on every supported host.
`SettingsSurface` independently attempts the optional native form; successful
registration never hides or replaces the menu. This supersedes the original
plan's single-surface policy following the maintainer's usability correction.
Older hosts retain all menu functions without linking `SettingsPanel`.

Only `NativeSettingsIntegration` and `SharpenerSettingsPanel` link the optional
Settings API. Native failure, missing classes/methods and missing navigation
leave the menu intact. Partial registration cleanup and late handles after
unload remain owned by this instance. A menu API failure does not prevent the
optional form or independent HTTP/editor features from loading.

The optional page contains configuration and the shortcut editor, with Global
and Project tabs, full-width sections, aligned fields, compact theme/size/debug/
icon dropdowns and a responsive tool checkbox grid. It does not reproduce menu
nesting or place help/unload commands in Settings. Shared actions retain existing
validation, serialization, scope, immediate effects and cancellation behavior.

Preference listeners, queued refreshes, both registrations and owned dialogs
are removed idempotently on unload. Burp's Settings window is never disposed by
extension cleanup. The duplicate-instance guard and existing native platform,
Dock and icon restoration behavior remain intact. The `hasTopMenu` flag remains
enabled and controls this configuration lifecycle. No preference migration is
needed to roll back.

## Evidence and limits

Exact automated/live results and source/jar identities are recorded in
[settings-usability-validation.json](settings-usability-validation.json).
The initial migration's separate historical results remain in
[native-settings-validation.json](native-settings-validation.json).
Headless tests cover shared writes/scopes, unchanged JSON/defaults, read-only
render/refresh, cancellation/rejection, update opt-in, optional API absence,
partial registration, navigation, fallback availability and cleanup ownership.
They do not establish native desktop compatibility.

Live search/discovery, dialog modality/focus, native effects, physical shortcut
conflicts, save/reopen, theme/DPI layouts and unload/reload require actual host
evidence. Current Community, Early Adopter and macOS/Linux Stable/EA evidence
remains pending; no 4.93 deferral or successful local build approves this new UI
or a future publication. Keep raw reports under ignored `temp/full-ui-test-reports/`.

Upstream API references checked 2026-11-19:
[SettingsPanel](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/ui/settings/SettingsPanel.html),
[official changelog](https://github.com/PortSwigger/burp-extensions-montoya-api/blob/main/CHANGELOG.md),
[settings tutorial](https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/tutorials/settings-panel).
Panel registration, builders and navigation were introduced separately. This
implementation uses the existing pinned API and custom component, without the
builder's automatic persistence or a claim that every host since introduction
has been tested.

The initial migration's last Windows Professional run reports build `20261102000055152` (2026.11.2
Stable) on the installed launcher and records 25 PASS, zero FAIL, clipboard
BLOCKED, one disabled editor N/A and eight manual groups required. It exercised
native discovery, every enabled settings control or a named required manual
case, immediate changes, title/icon chooser accept/cancel, shortcuts, context
navigation, and unload while Settings and a Sharpener input dialog were open.
The owned dialog closed and Burp Settings remained open. This is an incomplete
full suite, not a full pass. Earlier 2026.11.1 runs retain their separate jar and
host identities, including the initial ownership/timing assertion failures.
The installed host changed between runs; no installation was modified by this
implementation task.

`build` and `fullUiTestPlan` passed offline with strict dependency verification,
359 headless tests, packaged-resource/notice checks and the reviewed JDK 21
composition. No review dates/dispositions, release approvals or version were
refreshed to imply publication readiness. Physical search/keyboard/DPI/theme
checks, project save/reopen, remaining host matrix coverage remain pending. The usability correction adds
light/dark width previews and an actual native-form snapshot; these do not waive
physical desktop or other-host tests. The README labels the existing menu screenshots as
historical rather than presenting them as native-panel evidence.

The separately attempted, hash-verified Community 2026.3.3 run used Java 21,
a temporary project and no Professional licence/cache data. Startup timed out
before the harness loaded, so no assertions ran and the actual edition could
not be verified. The blocked report is retained; it supplies no Community pass.


## Menu restoration and form validation (2026-11-20)

The corrected implementation passed `build` and `fullUiTestPlan` offline with
strict dependency verification and 360 headless tests. Layout checks render
Global/Project pages in light/dark themes at 460px and 740px, including a regression
assertion for project fields pushed outside the viewport by wrapped help text.

The final Windows Professional build `20261102000055152` run recorded 26 PASS,
zero FAIL, one clipboard BLOCKED, one disabled editor N/A and eight manual groups
required. It verifies the menu caption is visible in Burp's real menu bar and
exercises both the full menu and the optional native form independently. Earlier
revised-form ownership/coverage failures remain in the separate evidence record.
The chooser now belongs to the window of its source control, including when
Settings is not Burp's active main-frame descendant.

The [Global screenshot](../images/sharpener-settings.png) is from the isolated
Professional host; the [Project layout preview](../images/sharpener-project-settings-preview.png)
is a synthetic headless render. These do not establish physical DPI/theme,
current Community or other-platform compatibility. The suite remains incomplete
while clipboard/manual/host matrix work is pending; no release is approved.
