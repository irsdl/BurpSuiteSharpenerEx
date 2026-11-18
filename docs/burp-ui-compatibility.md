# Burp UI compatibility

Sharpener targets both Stable and Early Adopter Burp releases. Prefer Montoya's
documented APIs for extension menus, editors, context menus, tabs and access to
the suite frame. Features that alter Burp's own tab headers still depend on
undocumented Swing structures; compatibility with every future UI cannot be
guaranteed.

As checked on 2026-11-16, the current host targets are
[Stable 2026.9.3](https://portswigger.net/burp/releases/professional-community-2026-9-3)
and [Early Adopter 2026.11](https://portswigger.net/burp/releases/professional-community-2026-11).
These identify validation targets, not claims that live host testing was completed.

## Behaviour when the UI changes

- Discovery starts from Montoya's `swingUtils().suiteFrame()`. It tolerates
  wrapper panels, reordered siblings and new tool names. Main tabs require at
  least two distinct recognised tool titles. Searches stop at tab boundaries,
  choose the nearest candidate layer, reject ambiguity within that layer and
  have depth/component limits. An unrelated deeper sidebar cannot invalidate a
  discovered pane. They never select a random
  visible frame as the main Burp window.
- Tool tabs are discovered inside their named main tool, or in a uniquely
  matching detached tool window. Tool ownership follows ancestors through wrapper
  panels. Unknown or ambiguous panes are left untouched. Renaming all recognised
  tool titles or replacing Swing tabs entirely will disable those customisations.
- Cached main tabs are checked against the current API frame/content. Tool panes
  are discovered anew. A coalescing Swing timer checks pane identities every two
  seconds and reattaches tab features when panes appear or are replaced. It stops
  during unload. It does not retry arbitrary failed settings or API registrations.
- Discovery, startup UI registration, settings UI changes and rebinding run on
  Swing's event thread. Readiness retries do not sleep on that thread and preserve
  interruption. The existing delayed tab rescan debounces container/header changes.
- Optional settings and individual API registrations have independent failure
  boundaries, including missing API methods/classes. Failed settings attempt to
  clean up their partial hooks. Core settings failure still aborts startup.
- Compatibility warnings name the affected feature and Burp build in the extension
  error log. Repeated warnings for the same feature are suppressed for that load.
  Saved preferences remain available for a compatible layout or a later release.
- Listeners are removed by their exact identities from the actual panes/headers
  they were attached to. Host property, component, container, mouse, keyboard and
  selection listeners are preserved regardless of their number or order.
- Icons are marked as Sharpener-owned. Native labels and unfamiliar header
  controls are preserved; unsupported icon/header layouts are skipped. Persistence
  refuses a style snapshot when any live tab lacks a valid recognised handler,
  including default, replaced and unknown headers. Empty panes during rebuilding
  and legacy preference snapshots are preserved rather than treated as evidence
  that the user deleted their styles.
- Duplicate detection still uses a liveness marker on the API frame's root pane.
  It is cleared from the original root during unload. If Burp no longer supplies
  a Swing frame, that UI-based guard and frame customisations are unavailable;
  independent Montoya integrations can still be attempted.

The existing light/dark look-and-feel change path still requests unloading and
manual reload. Automatic rebinding handles pane replacement; it does not claim
to make a live theme switch safe for all Burp/custom Swing delegates.

## Validation

Automated tests cover mixed/empty menus, unknown/new tab names, direct and deeply
wrapped layouts, ambiguity/search limits, replaced panes and menus, optional
API linkage failures, partial settings cleanup, preservation of host listeners,
late UI recovery, native header controls and protection against incomplete style
saves. Existing tab styling, shortcuts, editors and preferences tests remain in
the full offline build.

Before publishing, exercise the same jar in both current channels, using a fresh
temporary project without real traffic. Record the exact build number, edition,
OS, JRE, jar SHA-256 and results:

1. Load once and check settings/menu/editors/context menus and the extension log.
2. Create, rename, close, reorder and group Repeater and Intruder tabs; apply styles,
   icons and shortcuts. Include a completely empty tool and the first created tab.
3. Detach/reattach each tool and confirm recovery without duplicate listeners or
   loss of styles. Confirm Burp's native selection, close and keyboard controls.
4. Save/reopen the temporary project and confirm preference compatibility.
5. Unload, reload immediately, then try a duplicate copy. Check that unloading the
   duplicate does not alter the live copy or the saved settings.
6. Switch light/dark theme, follow the manual reload prompt, and confirm restoration
   and a usable menu. Unload after an unavailable feature and check cleanup.

For build **20261100000055090**, a read-only probe of the running Early Adopter
host reproduced `findMainTabs()` returning null after finding the real 15-tab
pane at depth 2: an unrelated sidebar reached depth 16 and discarded the match.
Regression tests cover both sibling orders, equivalent tool-pane discovery and
equally near ambiguity. The patched discovery is also checked against that live
component tree, without changing host UI/settings or inspecting messages.

On 2026-11-17, the installed Windows Early Adopter 2026.11 harness loaded and
unloaded the current jar: 23 case groups passed, with clipboard testing blocked,
one disabled editor N/A and eight desktop/manual cases pending. See
[the live results](full-ui-validation.json). Stable and the full OS/channel
matrix, including real host persistence/reload, remain unverified.
Community 2026.3.3 on Windows was also exercised in a temporary project: the same
21 case groups passed, with clipboard/manual coverage pending. Its jar checksum
matches the [official 2026.3.3 release](https://portswigger.net/burp/releases/professional-community-2026-3-3).
The test runner excludes Pro licensing and checks `COMMUNITY_EDITION` explicitly.
Headless Swing and mocked Montoya tests establish defensive behaviour, not proof
of host compatibility. See [dependency review](dependency-review.md) for the
separate unresolved build/CI admission and provenance gates.
