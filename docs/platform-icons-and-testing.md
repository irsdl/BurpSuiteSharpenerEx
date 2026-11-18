# Platform icons and dynamic host testing

Sharpener targets Windows, macOS and Linux on current Burp Stable and Early
Adopter. Platform checks must guard native calls; unsupported taskbar features
must leave portable functionality usable. Real host results are required before
claiming all-platform compatibility. The local validation below covers Windows.

## Windows installed launcher

A Java window's icons and an AppUserModelID are insufficient to guarantee a
taskbar icon for a native installed launcher. Microsoft documents how
[RelaunchIconResource](https://learn.microsoft.com/en-us/windows/win32/properties/props-system-appusermodel-relaunchiconresource)
selects a group's icon when an explicit window identity is present. Without it,
Windows may use a matching shortcut or the executable backing the process.

Sharpener now generates a real multi-resolution PNG-backed ICO using JDK codecs,
then sets `System.AppUserModel.RelaunchIconResource` before its window identity.
Original values of both properties, each window's Java icon list, and the exact
refresh listener/frame are retained and restored on reset/unload. Icon generation
runs on the owned background queue; stale queued updates cannot override reset.
No launcher executable, registry association or existing pinned shortcut is edited.
Existing pinned launcher shortcuts can retain their original icon; the running
custom group has separate identity. Explorer cache/pinned presentation still
requires visual validation.

Generated ICO files are retained under the runtime user's
`.burp-sharpener/icons/` directory, named by SHA-256 of their contents. Reusing an
icon reuses its resource. Retention is deliberate: a shortcut the user pins can
reference this path after the extension unloads. Removing these cached resources
can invalidate such shortcuts. This cache contains only user-selected icons,
not traffic or project contents. No taskbar pin is created automatically.

Windows native integration uses the existing core JNA dependency. No new codec,
native dependency, installer or runtime download was introduced. Windows tests
validate ICO directory/PNG offsets, repair/reuse of cached files and native guards.
On macOS, the existing supported `Taskbar.Feature.ICON_IMAGE` path updates and
restores the Dock icon. Linux retains portable Swing window icons; launcher and
desktop-environment behaviour varies. Neither platform invokes Windows APIs.

## Local dynamic validation on 2026-11-16

- Started the existing installed `BurpSuite.exe` in a dedicated temporary data
  directory/project, with extension autoload and auto-update disabled. Java debug
  transport listened on **127.0.0.1:5017**, not external interfaces. No installed
  launcher configuration or saved user project was modified.
- Used a JDK Attach agent to run the packaged `BurpTitleAndIcon.setIcon` and
  `resetIcon` paths against the actual main Burp frame, with a small validation
  parameter adapter and an auxiliary displayable Swing window. The adapter
  supplies the frame/log sink; this is not a full extension initialization test.
- Verified both native shell properties on the installed EXE's main window and
  the auxiliary window, plus all six Java icon sizes. Windows `LoadImageW` accepted
  the ICO at 16/24/32/48/64/128 pixels; all returned native icon handles were freed.
- Reset restored original main-window shell properties and Java icons, and
  cleared the extension's listener/snapshots. The validation worker and timer
  were stopped, and the test's auxiliary window disposed.
- JDI successfully attached to and detached from the loopback debug listener.
  The owned validation process was then closed; no listening debug socket remains.
  The installed launcher executable's SHA-256 was unchanged before/after testing.
- Computer-use access to Burp was not approved, so taskbar presentation was not
  visually verified. No alternative GUI automation was used. Native/backend
  validation proceeded within the requested debugging scope.

The host is Windows x64, Burp **2026.11 build 20261100000055090**, with its bundled
OpenJDK **26.0.2+10-55**. The extension itself builds for Java 21. The installed
host JRE is a test target, not a newly admitted release-build toolchain.
The tested extension jar SHA-256 is
`b5275106d2ab0568606fb7468c342070e976cbbd7016819101c25478b30df806`;
the offline build passed **299 tests**, with no failures/errors/skips.

## Repeating a test without changing the installation

Resolve the installed launcher and create a private temporary data/project path.
Reuse the existing local activation; never activate a disposable profile.
The [full UI harness](full-ui-tests.md) preserves cached activation in memory and
stops if an activation prompt appears, without changing registry/preferences.
The install4j launcher accepts JVM options with the
[`-J` prefix](https://www.ej-technologies.com/resources/install4j/help/doc/concepts/vmParameters.html).
Pass `-J-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5017`
for a loopback debug listener, alongside Burp's documented
[`--disable-extensions`, `--data-dir` and `--project-file` options](https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line)
(check that installed version's `--help`; newer flags may not appear in older docs).
Do not use `--use-defaults`: it can overwrite saved settings. Use a dedicated
test project, avoid real traffic, record any listeners, and close the owned test
instance/debugger afterward. Java Attach can inspect a running host without a
debug listener; never log message contents, arbitrary titles or license data.

Before release, record the exact jar hash and functional/load/unload/reload,
offline, icon/reset and desktop integration results on **each** of Windows,
macOS and Linux for **both** Stable and Early Adopter. Each host record in the
release evidence needs `channel`, `os_family` (`windows`, `macos`, `linux`),
`burp_build`, `edition`, `os`, `jre`, `reviewer`, `tested_utc`, `evidence`,
`jar_sha256`, `load_unload_reload_passed` and
`functional_and_large_project_checks_passed`. Mocked macOS/Linux branches are
regressions, not live host evidence. See [PortSwigger compliance](portswigger-compliance.md).
