# Change History

Release notes for the Sharpener Burp Suite extension. The newest version is at the top.

The CI release job copies the section of the released version into the GitHub release notes.
Keep the heading format exactly as `## Version X.Y (YYYY-MM-DD)` so it can find the section.

## Version 4.9 (2026-07-15)

- The off screen window check can now also restore a Burp window that has become too small to see: it is resized to two thirds of the screen and moved to the center.
- The off screen check no longer reacts to a minimised window. On Windows, minimising Burp used to save a wrong window position (-32000) and could show the recenter warning. "Use Last Screen Position And Size" now also ignores such a saved position or a saved size that is too small, so Burp can no longer start invisible.
- Moving the window to the center (including the Center Burp Window shortcut) now leaves room for the taskbar and keeps the title bar on the screen even when the window is larger than the screen.
- The off screen check now runs on the UI thread, and an error or an open question dialog in it can no longer stop the extension's shared timer, which also runs other delayed tasks.
- A new "Keyboard Shortcuts" dialog shows every Sharpener shortcut and lets you change it. Open it from the Sharpener top menu or from the bottom of a tab right-click menu. Changes are applied right away, no reload is needed.
- Each action now has ONE shortcut, set by clicking the cell and pressing the key combination (no more typing key names). If the key is already used, the dialog offers to move it. Burp's own hotkeys (Ctrl+R, Ctrl+I, ...) and basic editing keys cannot be taken.
- New navigation keys that work from anywhere, even while typing in the message editor: Ctrl+PageUp/Ctrl+PageDown for the previous/next tab and Alt+Home/Alt+End for the first/last tab. The old window-wide keys often did nothing because the editor swallowed them.
- New: press Down on a tab header to jump straight into the HTTP request editor and start editing. Press Alt+Up (or click the tab) to move the focus from the request or response editor back to the tab.
- The plain arrow, Home, End, and Down keys work as fixed tab-header navigation when a tab title has the focus, so they never disturb the editor. After a keyboard jump that started on the tab header, the focus stays there, so pressing an arrow key several times keeps navigating.
- Ctrl+C and Ctrl+V are not assigned by default, because a plain Ctrl+V could silently replace a tab title. Copy and paste titles use Ctrl+Shift+C and Ctrl+Shift+V, and a pasted title can be reverted from the tab menu under Previous Titles.
- Fixed: pressing Ctrl+V while the focus was on a side component could silently rename the current tab, and the Down arrow used to open the tab menu unexpectedly.
- Fixed: pressing Down right after an arrow key could bounce the focus back to the tab header instead of staying in the request editor, which could then send the next key to the wrong tab.
- The Back/Forward history now records every tab selection, including tabs selected by Burp itself (for example "Send to Repeater") and native navigation, and its size is capped.
- The tab context menu shows the current shortcut next to each action, reflecting your customisations.
- Unloading the extension now removes all key bindings cleanly and restores the native Burp behaviour, including entries left behind by older Sharpener versions.
- The tab menu now scrolls when it is taller than the screen, so the last items are no longer lost on short displays.
- The About menu item now shows the square Sharpener icon at a normal menu size. The old image was rotated and cropped, so it looked cut off.
- Fixed: clicking the theme icon of a main tool tab (for example Proxy), or the free space around the icon and the title, did not select that tab. These clicks now reach the tab bar.
- Main tool tab icons now sit closer to the title with no extra padding, so an icon no longer makes the tab bar taller or wider than needed.
- Unloading now fully restores the main tool tabs: Burp's own tab layout, title font, and border are put back exactly as they were.
- Fixed a memory leak: the focus listener that keeps the custom Burp icon applied is now removed by its exact instance on unload. The old code removed the last listener on the main window, which could belong to Burp or another extension, and the Sharpener listener could then stay behind after every reload.
- Fixed a memory leak: the "mouse wheel to scroll tabs" listener now has a named type, so unload removes only Sharpener listeners from the Repeater and Intruder tab bars. The old code removed the last wheel listener, which the look and feel may own. Turning the option on several times no longer stacks listeners either.
- Much faster tab menus: the icons shown in the tab right-click menu and in the "Change Burp Suite Icon" menu are now loaded once and cached. The old code rescanned the extension jar and reloaded every icon image on the UI thread each time a menu was opened, which made every tab right-click slow.
- The extension jar is about 2 MB smaller and loads faster: the Spring library was removed. It was only used to list the bundled icons, which a small helper now does.
- Extension loading can no longer stall for a long time when the Burp UI is slow to appear. The old retry logic could sleep for up to 45 seconds in total, the new one is capped at 5 seconds.

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
