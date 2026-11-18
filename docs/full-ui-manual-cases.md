# Manual part of the full UI suite

Use the exact jar and host recorded by the automated report, with a disposable
project and synthetic data. Record evidence for each case in the generated
checklist. These steps apply only when a full regression is requested.
On Community, use a temporary project. Disk-project save/reopen checks are
Professional-only: record that limitation as N/A, while still testing supported
global preferences and extension reload. Do not label unavailable Pro features
as Community failures or claim that they were exercised.

| Case | Procedure | Required result |
|---|---|---|
| `browser-links` | Click Project Page and Report Bug/Feature from Sharpener. | Correct repository/issue destinations open in the expected browser; cancelling/unsupported desktop browsing leaves Burp responsive. Never submit an issue as part of this test. |
| `update-network` | Toggle update checking on/off. Check equal/new/invalid versions, offline/timeout and a rejected TLS certificate using a controlled test endpoint/environment. Accept and decline the newer-version prompt. | Preference and checkbox agree; traffic uses Burp networking, TLS rejection is respected, only accepted handoff opens a browser, errors remain local and unload stops pending work. |
| `native-desktop` | Set every bundled icon and a custom portrait/square image; inspect main and genuinely detached windows, title bar and taskbar/Dock. Reset, unload and reload. | Windows installed launcher group uses the chosen icon; macOS Dock and Linux window icons follow supported capabilities. Original icons/properties return after reset/unload. Existing pinned shortcuts retain their own documented behaviour. |
| `desktop-shortcuts` | Open both shortcut dialog entry points. Edit/capture/save, close without saving, reset/confirm/cancel; try a duplicate shortcut, Ctrl+R/Ctrl+I/Ctrl+C/Ctrl+V and a plain typing key. Invoke each global and fixed header action from headers and editors. Try arrows/Home/End/Down, Alt+Up, Enter and all documented wheel/middle/Alt-click combinations. | Valid changes apply immediately; reserved/conflicting keys show useful feedback; cancel preserves bindings; editor typing/paste never renames a tab; focus and navigation land correctly without duplicate actions. |
| `detach-groups` | Create/close/reorder/group many Repeater and Intruder tabs, including empty tools. Detach/reattach each real tool, then create more tabs. Open menus through mouse and keyboard and scroll to their first/last entries. | Styles/icons/history follow intended titles; native close/group controls work; listeners recover once; all enabled actions remain reachable and no saved styles disappear during rebuilding. |
| `theme-monitors` | Exercise each theme/custom directory and icon size at high DPI/small resolution. Switch light/dark mode and follow the reload prompt. Move between monitors, restore/minimize, enable saved geometry/off-screen detection and test recovery with a removed monitor. | Menus/dialogs are parented and visible on the correct display; custom styles and normal controls restore cleanly; long/scrolling menus remain usable and Burp stays responsive. |
| `host-persistence-reload` | Change global/project styles and shortcuts. Save/reopen the real disposable Burp project; unload/reload immediately and attempt a duplicate copy. Cancel, then accept reset-all/unload. Try unsupported/temporarily missing tab layouts. | Settings persist in the real host, clean unload restores original UI/hooks, duplicate unload preserves the live copy, cancelled reset changes nothing, accepted reset removes only intended settings, unknown layouts retain snapshots. |
| `large-project` | Use many synthetic tabs, long titles and large local messages. Exercise regex find/replace/style, screenshots and menus repeatedly; unload with work or dialogs pending. Observe host logs, memory, CPU and event-thread responsiveness. | No unbounded retained requests/history/listeners/workers, UI freezes or repeated failures; screenshots remain local; all owned work/hooks stop. Record sizes, timing and resource measurements. |

For both supported highlighters, include synthetic local HTTP and WebSocket traffic
while toggling the capability and PwnFox header-removal controls. Verify highlight
colours, removal/retention of intended marker/header values, disabled behaviour
and unchanged unrelated messages. Do not send test traffic to external targets.

Include the menu-caption left/right-click shortcuts for Find Title and Jump To,
and open the actual request/response context menu in Proxy, Target and Repeater.
Check empty selections and messages without responses as well as the normal path.

Re-run on the other OS/channel combinations rather than copying evidence. The
Windows/mock branch tests do not establish actual macOS/Linux desktop behaviour.
