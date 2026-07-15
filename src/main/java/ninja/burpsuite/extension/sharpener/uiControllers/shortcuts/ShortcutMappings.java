// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.shortcuts;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Single source of truth for all Sharpener keyboard shortcuts.
 *
 * Model:
 * - Each action can have a FIXED tab-header key (a plain key like the arrows, Home, End,
 *   or Down). These only fire when a tab header has the focus, they never disturb the
 *   editor, and they are not user editable. They are built-in behaviour like the mouse
 *   gestures.
 * - Each configurable action has ONE user editable "anywhere" key. It works everywhere in
 *   the window, including while typing in the editor, so it needs a modifier (Ctrl, Alt,
 *   Meta) or a function key. This is what the Keyboard Shortcuts dialog edits.
 */
public final class ShortcutMappings {

    private ShortcutMappings() {
    }

    // Name of the preference that stores user overrides. The stored value is a map of
    // "actionKey.global" to a list holding zero or one keystroke string.
    // The "V2" suffix retires the earlier two-scope, multi-key storage from development
    // builds, so those incompatible saved values are ignored and the new defaults apply.
    public static final String CUSTOM_SHORTCUTS_SETTING_NAME = "customShortcutsV2";

    // All Sharpener entries in input maps and action maps start with this prefix,
    // so they can be found and removed without touching Burp or LAF entries.
    public static final String ACTION_KEY_PREFIX = "Sharpener";

    public static final String TAB_SCOPE_SUFFIX = ".tab";
    public static final String GLOBAL_SCOPE_SUFFIX = ".global";

    // Keep the selection history bounded so it cannot grow without limit.
    public static final int MAX_HISTORY_SIZE = 200;

    // Where a shortcut action is installed.
    public enum Target {
        SUB_TABS,   // the Repeater/Intruder tabbed pane
        BURP_FRAME  // the main Burp window
    }

    /**
     * One Sharpener action.
     *
     * @param fixedHeaderKey  plain key on the tab header, not user editable, "" when none
     * @param defaultGlobalKey the default "anywhere" key, "" when none by default
     * @param configurable    true when the action has an editable anywhere key shown in the dialog
     */
    public record ShortcutActionDef(String actionKey, String title, String description, Target target,
                                    String fixedHeaderKey, String defaultGlobalKey, boolean configurable) {
    }

    /** Resolved single key per action after user overrides are applied (values may be absent). */
    public record EffectiveShortcuts(Map<String, KeyStroke> headerKeys, Map<String, KeyStroke> globalKeys) {
    }

    private static final List<ShortcutActionDef> ACTION_DEFINITIONS = List.of(
            // Fixed tab-header navigation. These plain keys only fire when a tab header is
            // focused, so they never disturb the editor, and they are not user editable.
            new ShortcutActionDef(ACTION_KEY_PREFIX + "PreviousTab", "Previous Tab",
                    "Jumps to the previous visible tab.", Target.SUB_TABS,
                    "LEFT", "control PAGE_UP", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "NextTab", "Next Tab",
                    "Jumps to the next visible tab.", Target.SUB_TABS,
                    "RIGHT", "control PAGE_DOWN", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "FirstTab", "First Tab",
                    "Jumps to the first tab.", Target.SUB_TABS,
                    "HOME", "alt HOME", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "LastTab", "Last Tab",
                    "Jumps to the last tab.", Target.SUB_TABS,
                    "END", "alt END", true),
            // Down on the tab header drops into the request editor. Header only, not editable.
            new ShortcutActionDef(ACTION_KEY_PREFIX + "FocusRequestEditor", "Enter Request Editor",
                    "Moves the focus from the tab header into the HTTP request editor so it can be edited. Fixed key: Down.",
                    Target.SUB_TABS, "DOWN", "", false),
            // The reverse jump: from the request or response editor back to the tab header.
            new ShortcutActionDef(ACTION_KEY_PREFIX + "FocusTab", "Focus Tab",
                    "Moves the focus from the request or response editor back to the tab header.",
                    Target.SUB_TABS, "", "alt UP", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "ShowMenu", "Show Tab Menu",
                    "Opens the Sharpener menu of the selected tab (also middle-click or Alt+click a tab).",
                    Target.SUB_TABS, "", "control ENTER", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "FindTabs", "Find Tabs",
                    "Searches in tab titles using RegEx and jumps to the first match.",
                    Target.SUB_TABS, "", "control shift F", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "NextFind", "Find Next",
                    "Jumps to the next tab title that matches the last search.",
                    Target.SUB_TABS, "", "F3", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "PreviousFind", "Find Previous",
                    "Jumps to the previous tab title that matches the last search.",
                    Target.SUB_TABS, "", "shift F3", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "PreviouslySelectedTab", "Back",
                    "Jumps to the previously selected tab.", Target.SUB_TABS,
                    "", "alt LEFT", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "NextlySelectedTab", "Forward",
                    "Jumps forward in the tab selection history.", Target.SUB_TABS,
                    "", "alt RIGHT", true),
            // Copy and paste use Ctrl+Shift so a plain Ctrl+V can never replace a tab title
            // by accident. A pasted title stays recoverable under Previous Titles.
            new ShortcutActionDef(ACTION_KEY_PREFIX + "CopyTitle", "Copy Title",
                    "Copies the selected tab title to the clipboard.", Target.SUB_TABS,
                    "", "control shift C", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "PasteTitle", "Paste Title",
                    "Renames the selected tab using the clipboard text. The old title stays under Previous Titles.",
                    Target.SUB_TABS, "", "control shift V", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "RenameTitle", "Rename Title",
                    "Opens the rename box for the selected tab title.", Target.SUB_TABS,
                    "", "F2", true),
            new ShortcutActionDef(ACTION_KEY_PREFIX + "MoveToCenter", "Center Burp Window",
                    "Moves the Burp Suite window to the centre of the screen.", Target.BURP_FRAME,
                    "", "control alt C", true)
    );

    // Default hotkeys of Burp Suite itself. Users must not map a Sharpener action to these.
    // This list mirrors the Burp defaults; users can remap them inside Burp settings,
    // so this check is a safety net, not a full guarantee.
    private static final Map<String, String> BURP_RESERVED_KEYS = Map.ofEntries(
            Map.entry("control R", "Send to Repeater"),
            Map.entry("control I", "Send to Intruder"),
            Map.entry("control SPACE", "Send request"),
            Map.entry("control U", "URL-encode selection"),
            Map.entry("control shift U", "URL-decode selection"),
            Map.entry("control B", "Base64-encode selection"),
            Map.entry("control shift B", "Base64-decode selection"),
            Map.entry("control F", "Forward intercepted Proxy message"),
            Map.entry("control T", "Toggle Proxy interception"),
            Map.entry("control shift D", "Go to Dashboard"),
            Map.entry("control shift T", "Go to Target"),
            Map.entry("control shift P", "Go to Proxy"),
            Map.entry("control shift I", "Go to Intruder"),
            Map.entry("control shift R", "Go to Repeater")
    );

    // Basic editing keys. A configurable "anywhere" shortcut cannot use these because it
    // would break normal editing in the message editor.
    private static final Map<String, String> EDITING_KEYS_BLOCKED_GLOBALLY = Map.of(
            "control A", "Select all",
            "control C", "Copy",
            "control V", "Paste",
            "control X", "Cut",
            "control Z", "Undo",
            "control Y", "Redo"
    );

    // Keys and action names used by Sharpener 4.8 and older. They are cleaned during
    // install and uninstall so an upgrade does not leave stale entries behind.
    private static final List<String> LEGACY_KEYS = List.of(
            "control ENTER", "control shift ENTER", "DOWN", "control shift F",
            "F3", "control F3", "shift F3", "control shift F3",
            "HOME", "END", "control shift HOME", "control shift END",
            "LEFT", "RIGHT", "control shift LEFT", "control shift RIGHT",
            "alt LEFT", "alt RIGHT", "control alt LEFT", "control alt RIGHT",
            "control C", "control shift C", "control V", "control shift V",
            "F2", "control F2", "control alt C");

    private static final List<String> LEGACY_ACTION_KEYS = List.of(
            "ShowMenu", "FindTabs", "NextFind", "PreviousFind", "FirstTab", "LastTab",
            "PreviousTab", "NextTab", "PreviouslySelectedTab", "NextlySelectedTab",
            "CopyTitle", "PasteTitle", "RenameTitle", "MoveToCenter");

    public static List<ShortcutActionDef> getActionDefinitions() {
        return ACTION_DEFINITIONS;
    }

    /** Only the actions that appear in the Keyboard Shortcuts dialog (have an editable key). */
    public static List<ShortcutActionDef> getConfigurableActionDefinitions() {
        List<ShortcutActionDef> result = new ArrayList<>();
        for (ShortcutActionDef def : ACTION_DEFINITIONS) {
            if (def.configurable())
                result.add(def);
        }
        return result;
    }

    /** Actions that have a fixed tab-header key, for the read-only part of the dialog. */
    public static List<ShortcutActionDef> getFixedHeaderActionDefinitions() {
        List<ShortcutActionDef> result = new ArrayList<>();
        for (ShortcutActionDef def : ACTION_DEFINITIONS) {
            if (!def.fixedHeaderKey().isEmpty())
                result.add(def);
        }
        return result;
    }

    public static ShortcutActionDef getActionDefinition(String actionKey) {
        for (ShortcutActionDef def : ACTION_DEFINITIONS) {
            if (def.actionKey().equals(actionKey))
                return def;
        }
        return null;
    }

    /**
     * Reads the saved user overrides from the preferences.
     * Returns null when there is no override, so the defaults are used.
     */
    public static HashMap<String, ArrayList<String>> getSavedOverrides(ExtensionSharedParameters sharedParameters) {
        try {
            return sharedParameters.preferences.get(CUSTOM_SHORTCUTS_SETTING_NAME);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Merges the defaults with the saved overrides.
     * Fixed header keys always come from the definitions. A configurable key uses the saved
     * override when present, an empty override means the shortcut is disabled.
     */
    public static EffectiveShortcuts resolve(Map<String, ArrayList<String>> savedOverrides) {
        Map<String, KeyStroke> headerKeys = new LinkedHashMap<>();
        Map<String, KeyStroke> globalKeys = new LinkedHashMap<>();
        for (ShortcutActionDef def : ACTION_DEFINITIONS) {
            KeyStroke headerKey = parseKeyStroke(def.fixedHeaderKey());
            if (headerKey != null)
                headerKeys.put(def.actionKey(), headerKey);

            KeyStroke globalKey = resolveGlobalKey(savedOverrides, def);
            if (globalKey != null)
                globalKeys.put(def.actionKey(), globalKey);
        }
        return new EffectiveShortcuts(headerKeys, globalKeys);
    }

    private static KeyStroke resolveGlobalKey(Map<String, ArrayList<String>> savedOverrides, ShortcutActionDef def) {
        if (!def.configurable())
            return null;

        String settingKey = def.actionKey() + GLOBAL_SCOPE_SUFFIX;
        if (savedOverrides != null && savedOverrides.containsKey(settingKey)) {
            List<String> value = savedOverrides.get(settingKey);
            if (value == null || value.isEmpty())
                return null; // explicitly disabled by the user
            return parseKeyStroke(value.get(0));
        }
        return parseKeyStroke(def.defaultGlobalKey());
    }

    /**
     * Parses a keystroke from the Swing format ("control shift F") or
     * a human friendly format ("Ctrl+Shift+F"). Returns null when invalid or empty.
     */
    public static KeyStroke parseKeyStroke(String text) {
        if (text == null)
            return null;

        String cleaned = text.trim();
        if (cleaned.isEmpty())
            return null;

        // "pressed" appears in KeyStroke.toString() output, it is noise for parsing
        cleaned = cleaned.replaceAll("(?i)\\bpressed\\b", " ").trim();

        String[] parts = cleaned.split("[+\\s]+");
        StringBuilder swingFormat = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            boolean isLastPart = (i == parts.length - 1);
            String lower = parts[i].toLowerCase(Locale.ROOT);
            if (!isLastPart) {
                switch (lower) {
                    case "ctrl", "control" -> swingFormat.append("control ");
                    case "shift" -> swingFormat.append("shift ");
                    case "alt" -> swingFormat.append("alt ");
                    case "meta", "cmd", "command" -> swingFormat.append("meta ");
                    case "altgraph", "altgr" -> swingFormat.append("altGraph ");
                    default -> {
                        return null;
                    }
                }
            } else {
                swingFormat.append(normalizeKeyName(parts[i]));
            }
        }

        return KeyStroke.getKeyStroke(swingFormat.toString());
    }

    private static String normalizeKeyName(String keyName) {
        String upper = keyName.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "ESC" -> "ESCAPE";
            case "DEL" -> "DELETE";
            case "INS" -> "INSERT";
            case "RETURN" -> "ENTER";
            case "SPACEBAR" -> "SPACE";
            case "PGUP", "PAGEUP" -> "PAGE_UP";
            case "PGDN", "PGDOWN", "PAGEDOWN" -> "PAGE_DOWN";
            default -> upper;
        };
    }

    /** Formats a keystroke for display, for example "Ctrl+Shift+F". Empty string for null. */
    public static String formatKeyStroke(KeyStroke keyStroke) {
        if (keyStroke == null)
            return "";

        StringBuilder result = new StringBuilder();
        int modifiers = keyStroke.getModifiers();
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0)
            result.append("Ctrl+");
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0)
            result.append("Alt+");
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0)
            result.append("Shift+");
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0)
            result.append("Meta+");
        result.append(KeyEvent.getKeyText(keyStroke.getKeyCode()));
        return result.toString();
    }

    /** Canonical storage format that survives a save and load round trip. */
    public static String toStorageString(KeyStroke keyStroke) {
        StringBuilder result = new StringBuilder();
        int modifiers = keyStroke.getModifiers();
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0)
            result.append("control ");
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0)
            result.append("alt ");
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0)
            result.append("shift ");
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0)
            result.append("meta ");
        result.append(normalizeKeyName(keyTextForStorage(keyStroke.getKeyCode())));
        return result.toString();
    }

    private static String keyTextForStorage(int keyCode) {
        // KeyEvent.getKeyText can be localized, so VK field names are safer for storage.
        for (java.lang.reflect.Field field : KeyEvent.class.getFields()) {
            try {
                if (field.getName().startsWith("VK_") && field.getInt(null) == keyCode) {
                    return field.getName().substring(3);
                }
            } catch (IllegalAccessException e) {
                // not reachable for public static fields
            }
        }
        return KeyEvent.getKeyText(keyCode);
    }

    /** Menu label suffix such as " [Ctrl+C]", or an empty string when no key is set. */
    public static String menuHint(ExtensionSharedParameters sharedParameters, String actionKeyWithoutPrefix) {
        String actionKey = ACTION_KEY_PREFIX + actionKeyWithoutPrefix;
        EffectiveShortcuts effective = resolve(getSavedOverrides(sharedParameters));
        // prefer the simple tab-header key (Home, End, ...) for the hint, fall back to the anywhere key
        KeyStroke keyStroke = effective.headerKeys().get(actionKey);
        if (keyStroke == null)
            keyStroke = effective.globalKeys().get(actionKey);

        if (keyStroke == null)
            return "";
        return " [" + formatKeyStroke(keyStroke) + "]";
    }

    /** True when a keystroke is one of Burp's own default hotkeys. */
    public static String burpReservedName(KeyStroke keyStroke) {
        for (Map.Entry<String, String> entry : BURP_RESERVED_KEYS.entrySet()) {
            if (KeyStroke.getKeyStroke(entry.getKey()).equals(keyStroke))
                return entry.getValue();
        }
        return null;
    }

    /** True when a keystroke is a basic editing key that must not be taken globally. */
    public static String editingKeyName(KeyStroke keyStroke) {
        for (Map.Entry<String, String> entry : EDITING_KEYS_BLOCKED_GLOBALLY.entrySet()) {
            if (KeyStroke.getKeyStroke(entry.getKey()).equals(keyStroke))
                return entry.getValue();
        }
        return null;
    }

    /**
     * Checks one candidate "anywhere" key. Returns a human readable reason when it cannot be
     * used, or null when it is acceptable. Used by the dialog when a key is captured so the
     * user gets immediate feedback. Duplicate detection is handled separately by the dialog.
     */
    public static String rejectReasonForGlobalKey(KeyStroke keyStroke) {
        if (keyStroke == null)
            return null;

        String reserved = burpReservedName(keyStroke);
        if (reserved != null)
            return formatKeyStroke(keyStroke) + " is reserved by Burp Suite (" + reserved + ").";

        String editing = editingKeyName(keyStroke);
        if (editing != null)
            return formatKeyStroke(keyStroke) + " is a basic editing key (" + editing + ") and would break editing.";

        boolean hasStrongModifier = (keyStroke.getModifiers()
                & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0;
        boolean isFunctionKey = keyStroke.getKeyCode() >= KeyEvent.VK_F1 && keyStroke.getKeyCode() <= KeyEvent.VK_F24;
        if (!hasStrongModifier && !isFunctionKey)
            return formatKeyStroke(keyStroke) + " needs Ctrl, Alt, or a function key so it does not clash with normal typing.";

        return null;
    }

    /**
     * Validates the final set of configurable keys before saving. Returns an empty list when
     * everything is fine. This is a safety net; the dialog rejects most problems as they are typed.
     */
    public static List<String> validate(Map<String, KeyStroke> globalKeysByAction) {
        List<String> errors = new ArrayList<>();
        Map<KeyStroke, String> seen = new HashMap<>();

        for (Map.Entry<String, KeyStroke> entry : globalKeysByAction.entrySet()) {
            KeyStroke keyStroke = entry.getValue();
            if (keyStroke == null)
                continue;

            String reason = rejectReasonForGlobalKey(keyStroke);
            if (reason != null)
                errors.add(titleFor(entry.getKey()) + ": " + reason);

            String existing = seen.putIfAbsent(keyStroke, titleFor(entry.getKey()));
            if (existing != null)
                errors.add(formatKeyStroke(keyStroke) + " is used by both " + existing
                        + " and " + titleFor(entry.getKey()) + ".");
        }

        return errors;
    }

    private static String titleFor(String actionKey) {
        ShortcutActionDef def = getActionDefinition(actionKey);
        return def != null ? def.title() : actionKey;
    }

    /**
     * True when the keyboard focus is on the tab header area of the given tabbed pane,
     * or on the pane itself. False while a tab title is being renamed, so typing in the
     * rename box is never disturbed.
     */
    public static boolean isTabAreaFocused(JTabbedPane tabbedPane) {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        return isFocusOnTabHeader(focusOwner, tabbedPane);
    }

    // Pure logic behind isTabAreaFocused, split out so it can be unit tested with a real
    // component tree instead of the global keyboard focus manager.
    static boolean isFocusOnTabHeader(Component focusOwner, JTabbedPane tabbedPane) {
        if (focusOwner == null)
            return false;

        // a tab title being renamed is an editable field, typing in it must not navigate
        if (focusOwner instanceof JTextComponent textComponent && textComponent.isEditable())
            return false;

        if (focusOwner == tabbedPane)
            return true;

        // the focus is on the tab header area when it is inside one of the tab header
        // components (the title field and its buttons). Burp nests several components
        // inside a tab header, so a plain "direct child" check is not enough, a full
        // descendant check is used instead. Tab content (the request/response editors)
        // is never a tab header component, so it does not match.
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabHeader = tabbedPane.getTabComponentAt(i);
            if (tabHeader != null && SwingUtilities.isDescendingFrom(focusOwner, tabHeader))
                return true;
        }

        return false;
    }

    /**
     * Installs the sub-tab shortcuts on a tool tabbed pane.
     * Fixed header keys go to the focused and ancestor input maps with a focus guard.
     * The configurable "anywhere" key goes to the window input map without a guard.
     * Any previous Sharpener entries are removed first.
     */
    public static void installOnTabbedPane(JTabbedPane tabbedPane,
                                           Map<String, ArrayList<String>> savedOverrides,
                                           Map<String, Consumer<ActionEvent>> handlersByActionKey) {
        uninstallFromComponent(tabbedPane);

        EffectiveShortcuts effective = resolve(savedOverrides);

        for (ShortcutActionDef def : ACTION_DEFINITIONS) {
            if (def.target() != Target.SUB_TABS)
                continue;

            Consumer<ActionEvent> handler = handlersByActionKey.get(def.actionKey());
            if (handler == null)
                continue;

            KeyStroke headerKey = effective.headerKeys().get(def.actionKey());
            if (headerKey != null) {
                String mapKey = def.actionKey() + TAB_SCOPE_SUFFIX;
                tabbedPane.getActionMap().put(mapKey, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // only act when the tab header has the focus, so the message editor
                        // and other content components keep their own keys
                        if (isTabAreaFocused(tabbedPane))
                            handler.accept(e);
                    }
                });
                // the focused map covers the pane itself and shadows the LAF navigation,
                // the ancestor map covers the tab header components inside the pane
                tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).put(headerKey, mapKey);
                tabbedPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(headerKey, mapKey);
            }

            KeyStroke globalKey = effective.globalKeys().get(def.actionKey());
            if (globalKey != null) {
                String mapKey = def.actionKey() + GLOBAL_SCOPE_SUFFIX;
                tabbedPane.getActionMap().put(mapKey, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        handler.accept(e);
                    }
                });
                tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(globalKey, mapKey);
            }
        }
    }

    /**
     * Installs the Burp frame shortcuts on the root pane of the main window.
     * Any previous Sharpener entries are removed first.
     */
    public static void installOnBurpFrame(JRootPane rootPane,
                                          Map<String, ArrayList<String>> savedOverrides,
                                          Map<String, Consumer<ActionEvent>> handlersByActionKey) {
        uninstallFromComponent(rootPane);

        EffectiveShortcuts effective = resolve(savedOverrides);

        for (ShortcutActionDef def : ACTION_DEFINITIONS) {
            if (def.target() != Target.BURP_FRAME)
                continue;

            Consumer<ActionEvent> handler = handlersByActionKey.get(def.actionKey());
            if (handler == null)
                continue;

            KeyStroke globalKey = effective.globalKeys().get(def.actionKey());
            if (globalKey != null) {
                String mapKey = def.actionKey() + GLOBAL_SCOPE_SUFFIX;
                rootPane.getActionMap().put(mapKey, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        handler.accept(e);
                    }
                });
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(globalKey, mapKey);
            }
        }
    }

    /**
     * Removes every Sharpener entry from the input maps and the action map of the component.
     * Parent (LAF or Burp) entries are never touched, so the native behaviour is restored.
     * It also cleans entries left behind by Sharpener 4.8 and older.
     */
    public static void uninstallFromComponent(JComponent component) {
        int[] conditions = {JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                JComponent.WHEN_IN_FOCUSED_WINDOW};

        Set<KeyStroke> legacyKeyStrokes = new HashSet<>();
        for (String legacyKey : LEGACY_KEYS) {
            legacyKeyStrokes.add(KeyStroke.getKeyStroke(legacyKey));
        }

        for (int condition : conditions) {
            InputMap inputMap = component.getInputMap(condition);
            KeyStroke[] ownKeys = inputMap.keys();
            if (ownKeys == null)
                continue;

            for (KeyStroke keyStroke : ownKeys) {
                Object value = inputMap.get(keyStroke);
                if (!(value instanceof String stringValue))
                    continue;

                boolean isSharpenerEntry = stringValue.startsWith(ACTION_KEY_PREFIX);
                // old versions masked keys with "none" and used unprefixed action names
                boolean isLegacyEntry = legacyKeyStrokes.contains(keyStroke)
                        && (stringValue.equals("none") || LEGACY_ACTION_KEYS.contains(stringValue));
                if (isSharpenerEntry || isLegacyEntry) {
                    inputMap.remove(keyStroke);
                }
            }
        }

        ActionMap actionMap = component.getActionMap();
        Object[] actionKeys = actionMap.keys();
        if (actionKeys != null) {
            for (Object actionKey : actionKeys) {
                if (actionKey instanceof String stringKey
                        && (stringKey.startsWith(ACTION_KEY_PREFIX) || LEGACY_ACTION_KEYS.contains(stringKey))) {
                    actionMap.remove(actionKey);
                }
            }
        }
    }

    /**
     * Adds a selected tab index to the history with a size cap.
     * The last "..." tab is skipped when tab groups are not supported by Burp,
     * because that tab only creates a new tab.
     */
    public static void recordSelectionHistory(LinkedList<Integer> history, int indexNumber,
                                              boolean isLastTabIndex, boolean isTabGroupSupportedByDefault) {
        if (history == null)
            return;

        if (history.isEmpty()
                || (history.getLast() != indexNumber && (!isLastTabIndex || isTabGroupSupportedByDefault))) {
            history.add(indexNumber);
            while (history.size() > MAX_HISTORY_SIZE) {
                history.pollFirst();
            }
        }
    }
}
