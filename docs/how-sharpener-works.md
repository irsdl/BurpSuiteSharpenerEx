# How Sharpener Works

A complete technical reference for the Sharpener Burp Suite extension. It is written so that an AI agent or a new contributor can understand the whole solution without re-reading every source file. Keep it updated when behavior changes.

Related documents: `CLAUDE.md` (workflow rules), `.claude/burp-internals.md` (confirmed findings on Burp's obfuscated internals), `.claude/burp-live-debugging.md` (live debugging a running Burp), `docs/portswigger/` (BApp Store rules).

## 1. What Sharpener is

Sharpener is a Burp Suite extension (Java 21, Montoya API) that improves Burp's UI. Main feature areas:

- Sub-tab styling, renaming, searching, and navigation in Repeater and Intruder.
- A customizable keyboard shortcut system with a shortcuts dialog.
- Main tool tab styling with icon themes.
- Custom Burp window title and icon (including OS taskbar and Dock).
- Window position management (save/restore, off-screen recovery, centering).
- Proxy capabilities: PwnFox highlighter and manual request highlighting markers.

Hard constraints:

- **It cannot run standalone.** There is no `main()`. The Montoya API is `compileOnly`; Burp provides it at runtime.
- Much of the code manipulates Burp's internal Swing component tree using heuristics (class names, tab titles, window titles). Burp updates are the main source of breakage.
- Behavior can only be fully verified inside a running Burp. Headless tests cover logic and style values, not real UI integration.
- Tracks the latest Burp release only. Minimum version is gated in `extension.properties` (`minSupportedMajorVersionInclusive=2024`, `minSupportedMinorVersionInclusive=2`, the first Burp requiring Java 21).

## 2. Project structure

```
src/main/java/ninja/burpsuite/
  extension/sharpener/          The extension itself
    ExtensionMainClass.java     Entry point (BurpExtension + ExtensionUnloadingHandler)
    ExtensionSharedParameters.java  Shared state passed into nearly every constructor
    ExtensionGeneralSettings.java   Composes all *Settings, orchestrates load/unload
    capabilities/
      objects/                  Capability, CapabilitySettings, CapabilityGroup
      implementations/          Self-registering capabilities (PwnFox, ManualHighlighter)
    objects/                    TabFeaturesObject, TabFeaturesObjectStyle (persisted tab styles)
    uiControllers/              Code that modifies Burp's own UI
      burpFrame/                Window position/size, off-screen recovery
      mainTabs/                 Main tool tab styling and themes
      subTabs/                  Largest area: sub-tab styling, rename, search, navigation
      shortcuts/                ShortcutMappings, ShortcutsDialog
    uiSelf/                     UI the extension owns
      topMenu/                  The Sharpener menu in Burp's menu bar (main entry point)
      contextMenu/              Montoya context menu items provider
      suiteTab/                 Disabled feature (hasSuiteTab=false)
      httpRequestResponseEditor/  Disabled RSyntaxTextArea editor (flags false)
  libs/                         Generic reusable helpers, not Sharpener-specific
    burp/generic/               Burp version detection, title/icon, BurpUITools,
                                ExtendedPreferences, BurpExtensionSharedParameters
    generic/                    Swing helpers, UIWalker/UiSpecObject, ResourceIconCache,
                                DelayedTaskRunner, JScroll menus, mouse forwarders
    objects/                    StandardSettings, PreferenceObject
src/main/resources/
  extension.properties          Version, URLs, feature flags, min Burp version
  themes/<name>/<Tool>.png      Main tab icon themes
  subtabicons/                  Bundled sub-tab icons
  icons/                        Bundled Burp window icons
src/test/java/                  JUnit 5 + Mockito headless tests, mirrored package layout
```

Active vs legacy: `SubTabsListenersV2` and `SubTabsSettingsV2` are the active implementations. The non-V2 `SubTabsListeners`/`SubTabsSettings` are legacy reference only; do not extend them.

## 3. Startup and unload flow

### initialize (ExtensionMainClass)

1. Construct `ExtensionSharedParameters` (reads `extension.properties`, detects Burp version/edition, wraps preferences).
2. Set extension name, register the unloading handler.
3. Compatibility gate: if the Burp version is too old (or Community when not allowed), warn and self-unload.
4. `furtherLoadingChecks()`:
   - `setUIParametersUsingMontoya(10)`: resolve main frame, root tabbed pane, dark mode, original title and icons. Up to 10 attempts, 500 ms apart (load stall is capped at about 5 seconds).
   - Duplicate load detection (see below): if another live copy is already loaded, warn and self-unload without touching Burp's UI or saved settings.
   - Remove a stale Sharpener menu left by a previous quick unload if present.
   - Publish this copy's liveness marker on Burp's root pane so later copies can detect it.
   - `allSettings = new ExtensionGeneralSettings(...)`: triggers the full settings lifecycle and capability discovery.
   - Install a `UIManager` Look-and-Feel listener (see below).
5. Warm the icon cache on a background thread (`ResourceIconCache` for top menu and sub-tab icons) so menus open fast and the EDT never pays the first jar scan.
6. Register capability handlers with Montoya. For each `CapabilitySettings` in `allSettings.capabilitySettingsList`, its `CapabilityGroup` list decides registration: `PROXY_REQUEST_HANDLER`, `PROXY_RESPONSE_HANDLER`, `WEBSOCKET_CREATION_HANDLER`. Each registration reflects a fresh handler instance via `Capability.createCapabilityObject(...)`.
7. Register UI pieces gated by feature flags: context menu and top menu are on; suite tab and request/response editors are off. After registering the top menu, an `invokeLater` re-installs the menu UI delegate (fixes an unpainted menu after LaF change plus reload).

### Duplicate load detection (SingleInstanceGuard)

Loading Sharpener twice must not leave two menus and two copies fighting over the same Swing components and preference keys. A menu bar check alone cannot tell a stale leftover menu (a quick unload plus reload, where Burp removes the old menu only after the previous unload finishes) from a second live copy, so detection uses a liveness marker instead of the menu.

- The marker is a `java.util.function.BooleanSupplier` stored as a client property on Burp's main frame root pane (`ExtensionMainClass.INSTANCE_MARKER_KEY`). Each copy has its own classloader, so only JDK types cross the boundary; the supplier reports `!sharedParameters.isUnloaded()`, which turns false the instant unload begins (`delayedTasks.stop()` is the first line of unload).
- On load, `furtherLoadingChecks()` calls `SingleInstanceGuard.isAnotherInstanceLive(...)`. A live marker means a real second copy: the copy shows a warning, sets `isDuplicateInstance = true`, and self-unloads. `initialize()` returns early after `furtherLoadingChecks()` for a duplicate, so no handlers or UI are registered, and `unload()` returns early so it never runs the restore logic or clears the other copy's marker.
- A dead marker (supplier returns false), a missing marker, a foreign value, or a supplier that throws all count as "no live instance", so the stale leftover menu case falls through to the existing remove-and-continue path with no false positive.
- A non-duplicate copy publishes its own marker before loading settings and clears it on unload, but `SingleInstanceGuard.clear` only removes the marker when it is still this copy's own supplier, so a late unload of an old copy never removes a newer copy's marker. Clearing also drops the reference into the extension classloader, so an unloaded copy does not leak.
- Covered by `SingleInstanceGuardTest`. This replaces the old menu-bar-only check that unloaded after settings were already loaded (it left a half loaded extension behind on a quick reload, which is why it was removed in 4.6).

### Look-and-Feel change forces unload without saving

A LaF switch would corrupt saved styles, so the listener sets `unloadWithoutSave = true`, waits 2 seconds on the shared timer, switches Burp to the Extensions tab, warns the user to reload manually, and calls `extension().unload()`.

### Unload

`extensionUnloaded()` runs: stop the shared `delayedTasks` timer first (so no delayed task fires after unload, and so the liveness marker turns dead at once). A duplicate copy returns here without further work. Otherwise clear this copy's liveness marker, remove the LaF listener, `allSettings.unloadSettings()` (each area restores Burp's original UI), then flush the EDT with `invokeAndWait`. Unloading must leave Burp exactly as it was found.

## 4. Settings and preferences system

Each feature area has a `*Settings` class extending `StandardSettings` (`libs/objects`). The constructor runs the lifecycle in order:

1. `init()`
2. `definePreferenceObjectCollection()` returns `PreferenceObject`s (settingName, type, defaultValue, visibility)
3. `registerSettings()` (registers each preference; duplicates tolerated)
4. `loadSettings()`

`unloadSettings()` is called on extension unload and must restore original Burp state.

Persistence uses CoreyD97 BurpExtenderUtilities `Preferences` (Gson JSON through Burp preferences), subclassed as `ExtendedPreferences` which adds `safeSetSetting` (3 retries, write verification, auto-register on "not registered") and `safeGet*Setting` helpers that never throw.

Visibility: `GLOBAL` = Burp user preferences, shared across projects. `PROJECT` = stored in the project file. Most settings are GLOBAL; the window title and icon choices are PROJECT.

`ExtensionGeneralSettings` composes everything. Load order: `TopMenuSettings`, `ContextMenuSettings`, (`SuiteTabSettings` if enabled), optional update check, `BurpFrameSettings`, `MainTabsSettings`, `SubTabsSettingsV2`, then `capabilityInitializer()`.

Update check: gated by GLOBAL `checkForUpdate` (default false). Fetches `propertiesFileUrl` from GitHub through Burp networking with `RequestOptions.withUpstreamTLSVerification()` (Burp does not verify the upstream certificate otherwise), extracts `version=([\d.]+)`, compares both sides as `double`. This is why `version` in `extension.properties` must stay parseable as a double.

## 5. ExtensionSharedParameters (shared state)

Passed into nearly every constructor. Key members:

- Metadata: `version`, `extensionName`, `extensionURL`, `extensionIssueTracker`, `extensionPropertiesUrl`, `features` (flags), `preferences` (ExtendedPreferences), `montoyaApi`.
- Burp detection: `isBurpPro`, `burpMajorVersion` (year), `burpMinorVersion`, decoded from the 17-digit Montoya build number by `BurpVersionNumber` (`YYYYRRPPXXXBBBBBB`).
- UI accessors: `get_mainFrameUsingMontoya()` (the Burp `JFrame`), `get_mainMenuBarUsingMontoya()`, `get_rootTabbedPaneUsingMontoya()` (Burp's main tool tab pane, with fallbacks including a `UIWalker` search).
- `get_toolTabbedPane(MainTabs)`: returns the Repeater or Intruder inner `JTabbedPane`. Checks a validated cache, then the root pane by tab title, then, if the tool is detached into its own window, scans `Window.getWindows()` for a frame titled "Repeater"/"Intruder" and finds the pane with `UIWalker`/`UiSpecObject`.
- `delayedTasks`: one shared `DelayedTaskRunner` daemon timer used by every debounced or delayed job. `isUnloaded()` means this timer is stopped; every scheduled task must guard on it and swallow exceptions so it never kills the shared timer thread.
- Debug: GLOBAL int `debugLevel`, levels None(0), Verbose(1), VerboseAndPrefsRW(2), VeryVerbose(3). Enable via top menu Global Settings to get `printDebugMessage` output.
- Version-dependent behavior flags set in `loadingChecks()`: `isTabTextColorSetByBackground` is true for Burp older than 2026 (old Burp painted sub-tab title color via the background property; 2026+ uses `setForegroundAt`).
- Sub-tab state: supported tools set (Repeater, Intruder), per-tool handler lists, selection history lists, filter state, copied style, original UI delegates.

## 6. Sub-tabs (Repeater and Intruder)

The largest area. `SubTabsSettingsV2` (settings and persistence), `SubTabsListenersV2` (listener wiring), `SubTabsActions` (all action logic and the tab menu), `SubTabsContainerHandler` (wraps one tab's Swing components), `SubTabsCustomTabbedPaneUI` (custom FlatLaf UI delegate).

### The tab menu

Opened by right-click, middle-click, Alt+click on a tab, or Ctrl+Enter. Built by `SubTabsActions.createPopupMenu()` inside a `JScrollPopupMenu` (scrolls when taller than the screen). Contents:

- **Rename Title** (F2), plain input dialog, titles trimmed and capped at 100 chars.
- **Copy Title** (Ctrl+Shift+C) and **Paste Title** (Ctrl+Shift+V). Paste strips a leading `#<digits>` uniqueness prefix. The old title is kept under Previous Titles. The clipboard is always read on a background thread (`readClipboardTitleAsync`), never on the EDT: the clipboard owner is another process and a stuck owner would freeze the Burp UI. The Paste Title item starts disabled and is enabled when the value arrives.
- **Previous Titles**: per-tab title history with Set, Copy, Clear History.
- **Match/Replace Titles (RegEx)** across visible tabs.
- **Predefined Styles**: High/Medium/Low/Info Confirmed and Unconfirmed, Interesting 1/2 (bold, fixed colors: high `#f71414`, medium `#ff7e0d`, low `#FAD400`, info `#0d9e1e`, interesting `#395EEA`, interesting2 `#D641CF`), plus icon-only styles False Positive, Duplicate, Tick, Cross.
- **Custom Style**: font name (all system fonts), font size (10 to 40 step 2), bold, italic, icon (bundled `subtabicons` via `ResourceIconCache`), foreground color (JColorChooser, Swatches and RGB panels only).
- **Copy Style / Paste Style**, **Find/Paste Style (RegEx)**, **Paste Style For All Visible Tabs** (confirmation guarded), **Reset to Default**.
- **Find Title (RegEx)** (Ctrl+Shift+F), **Find Next** (F3), **Find Previous** (Shift+F3). Case-insensitive regex over visible titles.
- **Jump To**: First/Last/Previous/Next tab, plus **Back** (Alt+Left) and **Forward** (Alt+Right) over the selection history.
- **Capture Screenshot**: to clipboard or PNG file (`<title>_<timestamp>.png`).
- **Activate Mouse Wheel**: wheel scrolls tabs, Ctrl+wheel resizes title font (12 to 36).
- **Keyboard Shortcuts**: opens the shortcuts dialog.
- Each action shows its current shortcut, reflecting customizations.

Fixed mouse gestures (not configurable): middle/Alt click opens the menu; with Ctrl grows the font and bolds; with Ctrl+Shift shrinks; with Shift applies the big red bold style.

### Style persistence

Per-tab styles are stored as `TabFeaturesObject` (extends `TabFeaturesObjectStyle`: fontName, fontSize, isBold, isItalic, isCloseButtonVisible, iconResourceString, iconSize, colorCode hex) in a PROJECT map `TabFeaturesObject_Array_<tool>`, keyed by lowercase trimmed title. Only non-default tabs (or tabs with title history) are saved. `makeUniqueTitle()` prepends `#<n> ` to duplicate titles on save. A safety guard refuses to wipe a previously large style map with an empty one (protects against transient read failures). On load, styles are re-applied by title lookup through `SubTabsContainerHandler.updateByTabFeaturesObject(...)`.

Color handling is version-dependent: old Burp (`isTabTextColorSetByBackground`) sets title color through `setBackgroundAt` and uses a marker color to detect resets; Burp 2026+ uses `setForegroundAt`.

### Missed tab detection and delayed reloads

Burp sometimes adds tabs without firing the listener Sharpener relies on. Mitigations:

- `updateAllSubTabContainerHandlersObj` rebuilds handler lists when counts mismatch or a tab has no handler.
- When a user interacts with a tab that has no handler, `scheduleTabRescan(tool)` schedules a delayed load+save for that tool (deduplicated per tool via a concurrent set).
- Rescan delays: 1 s on Burp 2026+; on older Burp 3 s (Repeater) and 10 s (Intruder) because old Burp recolors tabs shortly after creation.
- All delayed work runs on `sharedParameters.delayedTasks` and dispatches UI mutation via `SwingUtilities.invokeLater`.

### Custom tabbed pane UI

`SubTabsCustomTabbedPaneUI` subclasses FlatLaf's `FlatTabbedPaneUI` to render filtered/hidden tabs at width 0, minimize tab height, and keep tabs in fixed position. Installed with a retrying non-blocking timer because Burp's own `BurpTabbedPaneUI` install can race and NPE. On modern Burp with native tab groups the custom UI is largely bypassed. Original UI delegates are kept for restore.

### Per-tool preferences (PROJECT unless noted)

- `TabFeaturesObject_Array_<tool>`: the style map.
- `isScrollable_<tool>` (default false), `mouseWheelToScroll_<tool>` (false), `minimizeSize_<tool>` (false), `isTabFixedPosition_<tool>` (true).

## 7. Keyboard shortcuts

`ShortcutMappings` is the single source of truth: action definitions, defaults, parsing/formatting, install/uninstall, validation, focus detection. `ShortcutsDialog` is the editor (opened from the top menu or the tab menu).

Two key scopes:

- **Fixed header keys**: plain keys that work only while a tab header has focus, never editable: Left/Right/Home/End navigate, Down enters the request editor. Installed on the tabbed pane's `WHEN_FOCUSED`/`WHEN_ANCESTOR` maps with an `isTabAreaFocused()` guard so they never fire while an editor or the rename box has focus.
- **Global (anywhere) keys**: one editable keystroke per action, installed `WHEN_IN_FOCUSED_WINDOW`, so they work even while typing in the message editor.

Actions and defaults (all prefixed `Sharpener` in the action maps):

| Action | Header key | Global default | Notes |
|---|---|---|---|
| Previous / Next Tab | Left / Right | Ctrl+PageUp / Ctrl+PageDown | skips hidden/disabled/group tabs |
| First / Last Tab | Home / End | Alt+Home / Alt+End | |
| Enter Request Editor | Down | none | not configurable; hit-tests into the editor |
| Focus Tab | none | Alt+Up | editor back to tab header, with a 250 ms retry to win Burp's focus race |
| Show Tab Menu | none | Ctrl+Enter | |
| Find Tabs / Next / Previous | none | Ctrl+Shift+F / F3 / Shift+F3 | |
| Back / Forward | none | Alt+Left / Alt+Right | selection history, capped at 200, recorded for every selection including Burp's own (Send to Repeater) |
| Copy / Paste Title | none | Ctrl+Shift+C / Ctrl+Shift+V | Ctrl+C/V deliberately unassigned |
| Rename Title | none | F2 | |
| Center Burp Window | none | Ctrl+Alt+C | targets the Burp frame, installed by BurpFrameListeners |

Storage: GLOBAL preference `customShortcutsV2`, a map of `<actionKey>.global` to a list of 0 or 1 keystroke strings (VK field names, locale safe). Empty list means explicitly disabled; absent means default. Validation rejects Burp reserved hotkeys (Ctrl+R, Ctrl+I, and others), plain editing keys (Ctrl+A/C/V/X/Z/Y), and any key without a strong modifier unless it is a function key. Assigning a key already in use offers to move it. Saving reloads bindings live, no extension reload.

Unload: `uninstallFromComponent()` removes only entries whose action value starts with `Sharpener`, plus known legacy 4.8-and-older entries, from all input map conditions and the action map. Parent, LaF, and Burp entries are untouched.

## 8. Main tool tabs (mainTabs)

`MainTabsStyleHandler` restyles chosen main tool tabs: saves the original layout/font/border/opaque as client properties, makes the title bold, and inserts a theme icon `JLabel` in a `FlowLayout` with tight spacing (no extra tab height). A `MouseEventForwarder` on the label and container keeps clicks on the icon and free space selecting the tab.

- Themes: bundled under `resources/themes/` (`@irsdl` default, halloween, game, hacker, gradient, mobster, office) with per-tool PNGs, plus an `extensions` fallback folder for known extension tabs, plus a custom directory option. Icon sizes 6 to 48 (default 16).
- Preferences (GLOBAL): `isUnique_<Tab>` per tool (default false), `ToolsThemeName` (default `@irsdl`), `ToolsIconSize` (default 16), `ToolsThemeCustomPath`, `isToolTabPaneScrollable` (default false).
- `MainTabsListeners` (ContainerListener on the root pane) re-applies styling 2 s after Burp adds tabs (debounced with a flag).
- Unload restores every tab's saved look and the tab layout policy, running on the EDT via `invokeAndWait` when needed.

## 9. Burp frame: window position, off-screen recovery

`BurpFrameSettings` + `BurpFrameListeners` (a ComponentListener on the main frame).

- **Save**: moves and resizes are debounced (1 s / 2 s) on the shared timer, then bounds are saved to GLOBAL `lastApplicationPosition` and `lastApplicationSize`. Minimised frames are never saved (Windows reports bogus -32000 bounds when iconified; checked via `Frame.ICONIFIED`).
- **Restore** (`useLastScreenPositionAndSize`, GLOBAL, default false): applied at load, ignoring saved positions beyond -30000 and sizes below `MIN_VISIBLE_FRAME_SIZE` (100x100), so Burp can never start invisible.
- **Off-screen check** (`detectOffScreenPosition`, GLOBAL, default true): at load, an interactive check (10% margin) asks before recentering; at runtime after moves/resizes, a non-interactive check (80% margin) recenters with a warning. A too-small window is resized to two thirds of the screen. The check runs on the EDT and any error or open dialog cannot stall the shared timer. Its dialogs use a null parent on purpose: the Burp frame is off the screen or invisible at that moment, and a dialog centered on that frame would be invisible too, so the dialog is centered on the primary screen instead.
- **Centering**: `UIHelper.moveFrameToCenter` centers within the default screen's usable bounds (taskbar insets subtracted) and clamps so the title bar stays reachable. Geometry helpers (`isBoundsOutOfScreen`, `getCenteredLocation`, `isSizeTooSmall`) are pure and unit-tested.
- Unload removes the listener and Sharpener key bindings; position/size preferences persist deliberately.

## 10. Burp title and icon

`BurpTitleAndIcon` (libs/burp/generic):

- Title: PROJECT preference `BurpTitle`, set via top menu, restored on unload from the original captured at startup.
- Icon: PROJECT `BurpIconCustomPath` (file) wins over PROJECT `BurpResourceIconName` (bundled icon); GLOBAL `LastBurpIconCustomPath` remembers the file dialog location. The base image is scaled to 16/24/32/48/64/128 and applied to every open `Window` (covers detached tools).
- OS integration: macOS Dock via `java.awt.Taskbar` (original saved once); Windows taskbar via `WindowsAppUserModelId`, a pure JNA COM property-store client that sets each frame's AppUserModelID to a custom ID so the taskbar shows the window icon instead of the install4j launcher icon. Empty ID restores the default. All of this is best effort, wrapped against `Exception | LinkageError`.
- Burp rewrites window icons, so a `WindowFocusListener` on the main frame re-applies the icons on focus gained and lost. The refresh is cheap: windows that already carry the icon list (compared by instance) are skipped, and the OS taskbar and dock calls only run when the applied icon actually changed (`appliedOsTaskbarIconImage`). The exact listener instance is stored so unload removes only Sharpener's listener (never Burp's or another extension's).
- Unload restores original icons on all windows, the Dock icon, and all recorded AppUserModelIDs.

## 11. Capabilities (proxy features)

Self-registering plug-in model in `capabilities/implementations/`:

- Each capability is a `CapabilitySettings` subclass declaring a `Capability` descriptor: name, description, `settingName` (GLOBAL boolean enabled flag), `capabilityGroupList`, `implementationClassName`, order, `enabledByDefault`.
- `ExtensionGeneralSettings.capabilityInitializer()` discovers subclasses with an org.reflections package scan of `capabilities.implementations`, instantiates each with `(ExtensionSharedParameters)`, and sorts by order. No manual registration anywhere.
- `ExtensionMainClass` registers handlers with Montoya per `CapabilityGroup`; handler classes are created reflectively and must expose a `(ExtensionSharedParameters, CapabilitySettings)` constructor.

Shipped capabilities (both enabled by default, toggled from the top menu):

- **PwnFox Highlighter** (`pwnFoxSupportCapability`, order 50000, PROXY_REQUEST_HANDLER). Reads the `X-PwnFox-Color` header from proxied requests (sent by the PwnFox Firefox extension) and sets the Burp highlight color. The header is kept readable during `handleRequestReceived` so other extensions can also read it, then removed in `handleRequestToBeSent` before the request leaves Burp, unless GLOBAL `pwnFoxRemoveColorHeader` (default true) is off.
- **Proxy Manual Highlighter** (`manualHighlightCapability`, order 10000, PROXY request + response + websocket handlers). The user types markers anywhere in a request: `tempcolorCOLORNAME` highlights the request and the marker is stripped before sending (body length recalculated); `permcolorCOLORNAME` stays in the request and highlights both the request and any response containing it. Supported color names: Red, Orange, Yellow, Green, Cyan, Blue, Pink, Magenta, Gray (case-insensitive). Also works for websocket text messages (temp only client to server).

## 12. UI owned by the extension (uiSelf)

- **Top menu** (`TopMenu`, the main user entry point, registered in Burp's menu bar; rebuilt on demand by `updateTopMenuBar()` on the EDT). Structure:
  - **Global Settings**: Tools' Template And Style (Enable All / Disable All, Icons' Theme radio group including a custom directory, Icons' Size radio group, per-tool `isUnique_<tool>` checkboxes; Extender/UserOptions/ProjectOptions are skipped, gone in Burp 2024.2+), Scrollable Tool Pane, Use Last Screen Position And Size, Detect Off Screen Window Position, **Supported Capabilities** (one checkbox per capability; PwnFox gets an extra "Remove the color header" item), and **Debug Settings** (radio group over the debug levels).
  - **Project Settings**: Change Burp Suite Title, Change Burp Suite Icon (bundled icons via `ResourceIconCache` or a custom file), Reset Burp Suite Title, Reset Burp Suite Icon.
  - **Keyboard Shortcuts**: opens `ShortcutsDialog`.
  - **Unload Extension** (confirm, then unload; a private one-shot timer named `SharpenerTopMenuCleanup`, deliberately not the shared timer because unload stops that one, force-removes the menu 5 s later if still present) and **Remove All Settings & Unload** (`preferences.resetAll()` then unload).
  - **Check for Update on Start** (toggles `checkForUpdate`; checks immediately when enabled), **Project Page** and **Report Bug/Feature** (open the browser), **About** (square `/sharpener.png` logo scaled to 24, shows name, version, copyright, URL).
- **Context menu** (`ContextMenu`): a Montoya `ContextMenuItemsProvider` for the HTTP message editor and message lists, active for Proxy, Target, and Repeater. Currently minimal (Print request / Print response to the extension output). Not to be confused with the rich tab-header menu, which lives in `SubTabsActions` (section 6); that menu appends shortcut hints via `ShortcutMappings.menuHint(...)`, preferring the header key over the global key.
- **Disabled features** (kept in code, gated off in `extension.properties`):
  - `suiteTab` (`hasSuiteTab=false`): `SuiteTab` is an empty `JComponent` placeholder for a future Sharpener dashboard tab.
  - `httpRequestResponseEditor` (`hasHttpRequestEditor`/`hasHttpResponseEditor` false): an experimental RSyntaxTextArea-based message editor. Working pieces: removes the shared `RTextAreaKeymap` so Burp shortcuts survive, re-installs Undo/Redo (Ctrl+Z/Y), Ctrl +/- font zoom following Burp's editor font, dark/light RSyntax theming (`RSyntaxUtils`), experimental Ctrl+Click copy and Ctrl+Alt+Click paste. Parts are still demo code (placeholder combo boxes), but `getRequest()` is safe: an untouched editor returns the original request unchanged and an edited one returns exactly the editor content, so nothing extra can reach a target even if the flag is enabled.

## 13. Library helpers (libs/generic)

- `UIWalker` + `UiSpecObject`: search a Swing component tree for a component matching a spec (class, visibility, and other conditions). Load-bearing for finding Burp's panes, especially detached tool windows.
- `ResourceIconCache`: lists and loads bundled icon resources from the extension jar once and caches them. Replaced a Spring classpath scan (removed in 4.9, jar about 2 MB smaller). Load-bearing for fast menus.
- `DelayedTaskRunner`: the single shared daemon timer behind `sharedParameters.delayedTasks`. Every delayed or debounced job in the extension runs here; tasks must guard `isUnloaded()` and never throw.
- `UIHelper`: dialogs (message/confirm/input/file/directory) and pure window geometry helpers (centering, off-screen, too-small checks).
- `ImageHelper`: image loading from resources or files, scaling, clipboard.
- `JScrollMenu` / `JScrollPopupMenu`: scrollable menu variants so long menus are usable on short screens.
- `JMenuItemKeepOpen`: a menu item that does not close the menu when clicked.
- `MouseEventForwarder`: forwards mouse events from a child component to a target (used so icon labels and tab padding still select tabs).
- `MouseAdapterExtensionHandler` / `MouseWheelListenerExtensionHandler`: named wrapper types for Sharpener's mouse listeners so unload can remove exactly Sharpener's listeners and never a Burp or LaF listener.
- `WindowsAppUserModelId`: JNA COM client for the Windows taskbar AppUserModelID property (see section 10).
- `HTTPMessageHelper`: a large stateless HTTP parsing and manipulation library (about 30 static methods: header/body split, header add/replace/remove, content-type/charset/boundary parsing, query and body parameter handling, cookie get/replace/add, verb replacement).
- `PropertiesHelper` (loads `.properties` resources), `Encoding` (URL-encoding helpers), `Utilities` (misc, including `isValidRegExPattern` used to validate user regex input before applying it).
- `libs/burp/generic`: `BurpUITools` (main tab enum, menu helpers, dark mode detection), `BurpVersionNumber` (build number decoding), `BurpTitleAndIcon`, `ExtendedPreferences`, `BurpExtensionSharedParameters` (base class of ExtensionSharedParameters), `BurpExtensionFeatures` (feature flag holder).

## 14. Threading and fragility rules

- All UI mutation must happen on the EDT (`SwingUtilities.invokeLater`; `invokeAndWait` on the unload path when off-EDT).
- Background work and debouncing go through the one shared `delayedTasks` timer. Tasks guard on `isUnloaded()` and swallow their own exceptions; one bad task must never kill the timer that runs everything else.
- Swing hacks find Burp components by class and title heuristics. After a Burp update breaks something, suspect changed component hierarchies or window titles first, and read `.claude/burp-internals.md` before digging.
- Unload must restore Burp's original UI exactly: original UI delegates, fonts, borders, layouts, title, icons, AppUserModelIDs, and only Sharpener's own listeners and key bindings removed (by exact instance or by the `Sharpener` action-name prefix).
- Ordering matters in load/unload code; several places intentionally wait (retry loops, delayed timers) for Burp's own UI updates to settle.

## 15. Testing conventions

- JUnit 5 and Mockito in `src/test/java`, mirrored package layout. Tests run headless in CI (`java.awt.headless=true`), so nothing may need a display; Swing components are constructed but never shown, and synthetic mouse events are dispatched directly.
- Standard mock pattern: `mock(ExtensionSharedParameters.class)` with nested mocks assigned to its public fields (`preferences`, `extensionClass`), stubbing `safeGet*Setting` and `get_rootTabbedPaneUsingMontoya()`. Tests flush the EDT with `SwingUtilities.invokeAndWait(() -> {})`; some deliberately run handlers off the EDT to mimic an unload started by Burp.
- All code needs regression tests, including UI style values: tab fonts, colors, icons, and themes are asserted (see `TabFeaturesObjectStyleTest`, `MainTabsStyleHandlerTest`, `SubTabsContainerHandlerIconTest`, `BurpTitleAndIconTest`, `UIHelperTest`). Restore paths assert original objects come back by instance (`assertSame` on saved layout/font/border).
- When fixing a bug, first add a test that reproduces it.
- Passing headless tests is not enough for UI-affecting changes: build the jar and verify inside a running Burp (see `CLAUDE.md` and `.claude/burp-live-debugging.md`).

## 16. Build, versioning, release (summary)

- `./gradlew jar` builds `releases/BurpSuiteSharpenerEx.jar` (fat jar; name comes from the project directory). JDK 21, Gradle 8.9.
- Version lives in `extension.properties` and must parse as a double. Every bump needs a `## Version X.Y (YYYY-MM-DD)` section at the top of `CHANGES.md`.
- `main` is push-protected; all changes go through PRs. Merging a version-bump PR publishes a rolling GitHub release tagged `main_<version>`.
- The BApp Store copy (`PortSwigger/sharpener`) is updated separately; see `docs/portswigger/` and the `bapp-store-update` skill.
