// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.shortcuts;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ShortcutMappingsTest {

    @Test
    void actionKeysAreUniqueAndPrefixed() {
        Set<String> actionKeys = new HashSet<>();
        for (ShortcutMappings.ShortcutActionDef def : ShortcutMappings.getActionDefinitions()) {
            assertTrue(def.actionKey().startsWith(ShortcutMappings.ACTION_KEY_PREFIX),
                    "Action key must be prefixed: " + def.actionKey());
            assertTrue(actionKeys.add(def.actionKey()), "Duplicate action key: " + def.actionKey());
            assertFalse(def.title().isBlank());
            assertFalse(def.description().isBlank());
        }
    }

    @Test
    void fixedHeaderKeysArePlainNavigationKeys() {
        Set<KeyStroke> allowed = new HashSet<>(List.of(
                KeyStroke.getKeyStroke("LEFT"), KeyStroke.getKeyStroke("RIGHT"),
                KeyStroke.getKeyStroke("HOME"), KeyStroke.getKeyStroke("END"),
                KeyStroke.getKeyStroke("DOWN")));
        for (ShortcutMappings.ShortcutActionDef def : ShortcutMappings.getFixedHeaderActionDefinitions()) {
            KeyStroke headerKey = ShortcutMappings.parseKeyStroke(def.fixedHeaderKey());
            assertNotNull(headerKey, "Invalid fixed header key: " + def.fixedHeaderKey());
            assertTrue(allowed.contains(headerKey), "Unexpected fixed header key: " + def.fixedHeaderKey());
        }
    }

    @Test
    void everyDefaultAnywhereKeyIsAcceptable() {
        Set<KeyStroke> seen = new HashSet<>();
        for (ShortcutMappings.ShortcutActionDef def : ShortcutMappings.getConfigurableActionDefinitions()) {
            if (def.defaultGlobalKey().isEmpty())
                continue;
            KeyStroke keyStroke = ShortcutMappings.parseKeyStroke(def.defaultGlobalKey());
            assertNotNull(keyStroke, "Invalid default anywhere key: " + def.defaultGlobalKey());
            assertNull(ShortcutMappings.rejectReasonForGlobalKey(keyStroke),
                    "Default anywhere key must be acceptable: " + def.defaultGlobalKey());
            assertTrue(seen.add(keyStroke), "Duplicate default anywhere key: " + def.defaultGlobalKey());
        }
    }

    @Test
    void copyAndPasteDoNotUsePlainCtrlKeys() {
        // a plain Ctrl+V default could silently replace a tab title, it must not be a default
        for (ShortcutMappings.ShortcutActionDef def : ShortcutMappings.getActionDefinitions()) {
            KeyStroke global = ShortcutMappings.parseKeyStroke(def.defaultGlobalKey());
            assertNotEquals(KeyStroke.getKeyStroke("control C"), global);
            assertNotEquals(KeyStroke.getKeyStroke("control V"), global);
        }
    }

    @Test
    void defaultMappingsPassValidation() {
        ShortcutMappings.EffectiveShortcuts effective = ShortcutMappings.resolve(null);
        assertTrue(ShortcutMappings.validate(effective.globalKeys()).isEmpty());
    }

    @Test
    void parseKeyStrokeAcceptsHumanAndSwingFormats() {
        KeyStroke expected = KeyStroke.getKeyStroke("control shift F");
        assertEquals(expected, ShortcutMappings.parseKeyStroke("Ctrl+Shift+F"));
        assertEquals(expected, ShortcutMappings.parseKeyStroke("control shift F"));
        assertEquals(expected, ShortcutMappings.parseKeyStroke(" ctrl + shift + f "));
        assertEquals(KeyStroke.getKeyStroke("F3"), ShortcutMappings.parseKeyStroke("F3"));
        assertEquals(KeyStroke.getKeyStroke("control PAGE_DOWN"), ShortcutMappings.parseKeyStroke("Ctrl+PageDown"));
        assertEquals(KeyStroke.getKeyStroke("alt UP"), ShortcutMappings.parseKeyStroke("Alt+Up"));
        assertNull(ShortcutMappings.parseKeyStroke("NotAKey+X"));
        assertNull(ShortcutMappings.parseKeyStroke(""));
        assertNull(ShortcutMappings.parseKeyStroke(null));
    }

    @Test
    void storageFormatSurvivesRoundTrip() {
        for (ShortcutMappings.ShortcutActionDef def : ShortcutMappings.getActionDefinitions()) {
            for (String keyText : List.of(def.fixedHeaderKey(), def.defaultGlobalKey())) {
                if (keyText.isEmpty())
                    continue;
                KeyStroke original = ShortcutMappings.parseKeyStroke(keyText);
                String stored = ShortcutMappings.toStorageString(original);
                assertEquals(original, ShortcutMappings.parseKeyStroke(stored),
                        "Round trip failed for: " + keyText + " stored as: " + stored);
            }
        }
    }

    @Test
    void formatKeyStrokeIsHumanReadable() {
        assertEquals("Ctrl+Shift+F", ShortcutMappings.formatKeyStroke(KeyStroke.getKeyStroke("control shift F")));
        assertEquals("F3", ShortcutMappings.formatKeyStroke(KeyStroke.getKeyStroke("F3")));
        assertEquals("Ctrl+Alt+C", ShortcutMappings.formatKeyStroke(KeyStroke.getKeyStroke("control alt C")));
        assertEquals("", ShortcutMappings.formatKeyStroke(null));
    }

    @Test
    void resolveUsesDefaultsWhenNoOverrides() {
        ShortcutMappings.EffectiveShortcuts effective = ShortcutMappings.resolve(null);
        String nextTabKey = ShortcutMappings.ACTION_KEY_PREFIX + "NextTab";
        assertEquals(KeyStroke.getKeyStroke("RIGHT"), effective.headerKeys().get(nextTabKey));
        assertEquals(KeyStroke.getKeyStroke("control PAGE_DOWN"), effective.globalKeys().get(nextTabKey));

        // the request-editor jump is header only, it has no anywhere key
        String focusEditorKey = ShortcutMappings.ACTION_KEY_PREFIX + "FocusRequestEditor";
        assertEquals(KeyStroke.getKeyStroke("DOWN"), effective.headerKeys().get(focusEditorKey));
        assertNull(effective.globalKeys().get(focusEditorKey));
    }

    @Test
    void resolveAppliesOverrideAndEmptyDisables() {
        String nextTabKey = ShortcutMappings.ACTION_KEY_PREFIX + "NextTab";
        HashMap<String, ArrayList<String>> overrides = new HashMap<>();
        overrides.put(nextTabKey + ShortcutMappings.GLOBAL_SCOPE_SUFFIX, new ArrayList<>(List.of("control alt N")));

        String showMenuKey = ShortcutMappings.ACTION_KEY_PREFIX + "ShowMenu";
        overrides.put(showMenuKey + ShortcutMappings.GLOBAL_SCOPE_SUFFIX, new ArrayList<>());

        ShortcutMappings.EffectiveShortcuts effective = ShortcutMappings.resolve(overrides);
        assertEquals(KeyStroke.getKeyStroke("control alt N"), effective.globalKeys().get(nextTabKey));
        // the fixed header key is never affected by an override
        assertEquals(KeyStroke.getKeyStroke("RIGHT"), effective.headerKeys().get(nextTabKey));
        // an empty override disables the anywhere key
        assertNull(effective.globalKeys().get(showMenuKey));
    }

    @Test
    void rejectReasonRejectsReservedEditingAndPlainKeys() {
        assertTrue(ShortcutMappings.rejectReasonForGlobalKey(KeyStroke.getKeyStroke("control R")).contains("reserved by Burp"));
        assertTrue(ShortcutMappings.rejectReasonForGlobalKey(KeyStroke.getKeyStroke("control C")).contains("editing"));
        assertTrue(ShortcutMappings.rejectReasonForGlobalKey(KeyStroke.getKeyStroke("X")).contains("needs Ctrl"));
        assertTrue(ShortcutMappings.rejectReasonForGlobalKey(KeyStroke.getKeyStroke("shift X")).contains("needs Ctrl"));

        assertNull(ShortcutMappings.rejectReasonForGlobalKey(KeyStroke.getKeyStroke("control alt N")));
        assertNull(ShortcutMappings.rejectReasonForGlobalKey(KeyStroke.getKeyStroke("F5")));
        assertNull(ShortcutMappings.rejectReasonForGlobalKey(KeyStroke.getKeyStroke("control PAGE_UP")));
    }

    @Test
    void validateRejectsDuplicateKeys() {
        Map<String, KeyStroke> globalKeys = new HashMap<>();
        globalKeys.put(ShortcutMappings.ACTION_KEY_PREFIX + "NextTab", KeyStroke.getKeyStroke("control alt N"));
        globalKeys.put(ShortcutMappings.ACTION_KEY_PREFIX + "PreviousTab", KeyStroke.getKeyStroke("control alt N"));

        List<String> errors = ShortcutMappings.validate(globalKeys);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("used by both"), "Got: " + errors.get(0));
    }

    @Test
    void validateAllowsNullKeys() {
        Map<String, KeyStroke> globalKeys = new HashMap<>();
        globalKeys.put(ShortcutMappings.ACTION_KEY_PREFIX + "NextTab", null);
        globalKeys.put(ShortcutMappings.ACTION_KEY_PREFIX + "PreviousTab", KeyStroke.getKeyStroke("control alt N"));
        assertTrue(ShortcutMappings.validate(globalKeys).isEmpty());
    }

    @Test
    void installPutsHeaderKeyInFocusedMapsAndGlobalKeyInWindowMap() {
        JTabbedPane tabbedPane = new JTabbedPane();
        ShortcutMappings.installOnTabbedPane(tabbedPane, null, allSubTabHandlers());

        String nextTabKey = ShortcutMappings.ACTION_KEY_PREFIX + "NextTab";
        KeyStroke right = KeyStroke.getKeyStroke("RIGHT");
        KeyStroke controlPageDown = KeyStroke.getKeyStroke("control PAGE_DOWN");

        assertEquals(nextTabKey + ShortcutMappings.TAB_SCOPE_SUFFIX,
                tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).get(right));
        assertEquals(nextTabKey + ShortcutMappings.TAB_SCOPE_SUFFIX,
                tabbedPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(right));
        assertEquals(nextTabKey + ShortcutMappings.GLOBAL_SCOPE_SUFFIX,
                tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(controlPageDown));

        assertNotNull(tabbedPane.getActionMap().get(nextTabKey + ShortcutMappings.TAB_SCOPE_SUFFIX));
        assertNotNull(tabbedPane.getActionMap().get(nextTabKey + ShortcutMappings.GLOBAL_SCOPE_SUFFIX));

        // the global key must not be in the focused map
        assertNull(tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).get(controlPageDown));
    }

    @Test
    void installFocusRequestEditorAsHeaderOnly() {
        JTabbedPane tabbedPane = new JTabbedPane();
        ShortcutMappings.installOnTabbedPane(tabbedPane, null, allSubTabHandlers());

        String focusEditorKey = ShortcutMappings.ACTION_KEY_PREFIX + "FocusRequestEditor";
        assertEquals(focusEditorKey + ShortcutMappings.TAB_SCOPE_SUFFIX,
                tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("DOWN")));
        // no global map key exists for it
        assertNull(tabbedPane.getActionMap().get(focusEditorKey + ShortcutMappings.GLOBAL_SCOPE_SUFFIX));
    }

    @Test
    void installRespectsOverride() {
        JTabbedPane tabbedPane = new JTabbedPane();
        String nextTabKey = ShortcutMappings.ACTION_KEY_PREFIX + "NextTab";
        HashMap<String, ArrayList<String>> overrides = new HashMap<>();
        overrides.put(nextTabKey + ShortcutMappings.GLOBAL_SCOPE_SUFFIX, new ArrayList<>(List.of("control alt N")));

        ShortcutMappings.installOnTabbedPane(tabbedPane, overrides, allSubTabHandlers());

        assertEquals(nextTabKey + ShortcutMappings.GLOBAL_SCOPE_SUFFIX,
                tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("control alt N")));
        // the old default anywhere key is gone
        Object oldValue = tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("control PAGE_DOWN"));
        assertFalse(oldValue instanceof String stringValue && stringValue.startsWith(ShortcutMappings.ACTION_KEY_PREFIX));
        // the fixed header key is still there
        assertEquals(nextTabKey + ShortcutMappings.TAB_SCOPE_SUFFIX,
                tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("RIGHT")));
    }

    @Test
    void uninstallRemovesSharpenerEntriesAndKeepsForeignEntries() {
        JTabbedPane tabbedPane = new JTabbedPane();

        KeyStroke foreignKey = KeyStroke.getKeyStroke("control alt shift Z");
        tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(foreignKey, "someBurpAction");
        Action foreignAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        tabbedPane.getActionMap().put("someBurpAction", foreignAction);

        ShortcutMappings.installOnTabbedPane(tabbedPane, null, allSubTabHandlers());
        ShortcutMappings.uninstallFromComponent(tabbedPane);

        int[] conditions = {JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                JComponent.WHEN_IN_FOCUSED_WINDOW};
        for (int condition : conditions) {
            KeyStroke[] ownKeys = tabbedPane.getInputMap(condition).keys();
            if (ownKeys == null)
                continue;
            for (KeyStroke keyStroke : ownKeys) {
                Object value = tabbedPane.getInputMap(condition).get(keyStroke);
                assertFalse(value instanceof String stringValue
                                && stringValue.startsWith(ShortcutMappings.ACTION_KEY_PREFIX),
                        "Sharpener entry left behind: " + value);
            }
        }

        assertEquals("someBurpAction", tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(foreignKey));
        assertSame(foreignAction, tabbedPane.getActionMap().get("someBurpAction"));
    }

    @Test
    void uninstallCleansLegacyEntries() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control C"), "none");
        tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt LEFT"), "PreviouslySelectedTab");
        tabbedPane.getActionMap().put("CopyTitle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        ShortcutMappings.uninstallFromComponent(tabbedPane);

        assertNull(tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("control C")));
        assertNull(tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("alt LEFT")));
        assertNull(tabbedPane.getActionMap().get("CopyTitle"));
    }

    @Test
    void globalActionRunsWithoutFocusButHeaderActionNeedsFocus() {
        JTabbedPane tabbedPane = new JTabbedPane();
        AtomicInteger callCount = new AtomicInteger();
        Map<String, Consumer<ActionEvent>> handlers = new HashMap<>();
        handlers.put(ShortcutMappings.ACTION_KEY_PREFIX + "NextTab", e -> callCount.incrementAndGet());

        ShortcutMappings.installOnTabbedPane(tabbedPane, null, handlers);

        String nextTabKey = ShortcutMappings.ACTION_KEY_PREFIX + "NextTab";
        ActionEvent event = new ActionEvent(tabbedPane, ActionEvent.ACTION_PERFORMED, "test");

        // in a headless test nothing has the focus, so the header action must not run
        tabbedPane.getActionMap().get(nextTabKey + ShortcutMappings.TAB_SCOPE_SUFFIX).actionPerformed(event);
        assertEquals(0, callCount.get());

        // the global action has no focus guard
        tabbedPane.getActionMap().get(nextTabKey + ShortcutMappings.GLOBAL_SCOPE_SUFFIX).actionPerformed(event);
        assertEquals(1, callCount.get());
    }

    @Test
    void installOnBurpFrameOnlyInstallsFrameActions() {
        JRootPane rootPane = new JRootPane();
        Map<String, Consumer<ActionEvent>> handlers = new HashMap<>();
        handlers.put(ShortcutMappings.ACTION_KEY_PREFIX + "MoveToCenter", e -> {
        });

        ShortcutMappings.installOnBurpFrame(rootPane, null, handlers);

        String moveToCenterKey = ShortcutMappings.ACTION_KEY_PREFIX + "MoveToCenter" + ShortcutMappings.GLOBAL_SCOPE_SUFFIX;
        assertEquals(moveToCenterKey,
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("control alt C")));
        assertNotNull(rootPane.getActionMap().get(moveToCenterKey));

        // sub-tab actions must not be installed on the frame
        assertNull(rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("control PAGE_DOWN")));

        ShortcutMappings.uninstallFromComponent(rootPane);
        assertNull(rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("control alt C")));
    }

    @Test
    void isFocusOnTabHeaderMatchesDeeplyNestedHeaderComponent() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("one", new JPanel());
        tabbedPane.addTab("two", new JPanel());

        JPanel header = new JPanel();
        JPanel middle = new JPanel();
        JButton deepChild = new JButton("x");
        middle.add(deepChild);
        header.add(middle);
        tabbedPane.setTabComponentAt(1, header);

        assertTrue(ShortcutMappings.isFocusOnTabHeader(deepChild, tabbedPane),
                "A deep descendant of a tab header must count as tab-area focus");
        assertTrue(ShortcutMappings.isFocusOnTabHeader(header, tabbedPane));
        assertTrue(ShortcutMappings.isFocusOnTabHeader(tabbedPane, tabbedPane));
    }

    @Test
    void isFocusOnTabHeaderIsFalseForContentAndNull() {
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel content = new JPanel();
        JButton contentButton = new JButton("go");
        content.add(contentButton);
        tabbedPane.addTab("one", content);
        tabbedPane.setTabComponentAt(0, new JPanel());

        assertFalse(ShortcutMappings.isFocusOnTabHeader(contentButton, tabbedPane));
        assertFalse(ShortcutMappings.isFocusOnTabHeader(null, tabbedPane));
    }

    @Test
    void isFocusOnTabHeaderIsFalseWhileRenaming() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("one", new JPanel());
        JPanel header = new JPanel();
        JTextField titleField = new JTextField("one");
        header.add(titleField);
        tabbedPane.setTabComponentAt(0, header);

        titleField.setEditable(true);
        assertFalse(ShortcutMappings.isFocusOnTabHeader(titleField, tabbedPane));

        titleField.setEditable(false);
        assertTrue(ShortcutMappings.isFocusOnTabHeader(titleField, tabbedPane));
    }

    @Test
    void recordSelectionHistoryAddsDedupsAndCaps() {
        LinkedList<Integer> history = new LinkedList<>();

        ShortcutMappings.recordSelectionHistory(history, 1, false, false);
        ShortcutMappings.recordSelectionHistory(history, 1, false, false);
        ShortcutMappings.recordSelectionHistory(history, 2, false, false);
        assertEquals(List.of(1, 2), history);

        ShortcutMappings.recordSelectionHistory(history, 5, true, false);
        assertEquals(List.of(1, 2), history);

        ShortcutMappings.recordSelectionHistory(history, 5, true, true);
        assertEquals(List.of(1, 2, 5), history);

        for (int i = 0; i < ShortcutMappings.MAX_HISTORY_SIZE * 2; i++) {
            ShortcutMappings.recordSelectionHistory(history, i % 2 == 0 ? 10 : 11, false, false);
        }
        assertEquals(ShortcutMappings.MAX_HISTORY_SIZE, history.size());
    }

    private static Map<String, Consumer<ActionEvent>> allSubTabHandlers() {
        Map<String, Consumer<ActionEvent>> handlers = new HashMap<>();
        for (ShortcutMappings.ShortcutActionDef def : ShortcutMappings.getActionDefinitions()) {
            if (def.target() == ShortcutMappings.Target.SUB_TABS) {
                handlers.put(def.actionKey(), e -> {
                });
            }
        }
        return handlers;
    }
}
