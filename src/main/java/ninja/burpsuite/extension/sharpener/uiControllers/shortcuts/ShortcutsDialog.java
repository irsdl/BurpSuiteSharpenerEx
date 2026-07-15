// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.shortcuts;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Dialog that shows every configurable Sharpener shortcut and lets the user change it.
 * Each action has ONE shortcut, set by pressing the key combination (no typing).
 * The fixed tab-header keys (arrows, Home, End, Down) are shown as read-only information.
 */
public final class ShortcutsDialog {

    private ShortcutsDialog() {
    }

    public static void show(ExtensionSharedParameters sharedParameters) {
        List<ShortcutMappings.ShortcutActionDef> configurable = ShortcutMappings.getConfigurableActionDefinitions();
        ShortcutMappings.EffectiveShortcuts effective =
                ShortcutMappings.resolve(ShortcutMappings.getSavedOverrides(sharedParameters));

        KeyStroke[] currentKeys = new KeyStroke[configurable.size()];
        for (int i = 0; i < configurable.size(); i++) {
            currentKeys[i] = effective.globalKeys().get(configurable.get(i).actionKey());
        }

        Component parent = sharedParameters.get_mainFrameUsingMontoya();
        ShortcutTableModel tableModel = new ShortcutTableModel(configurable, currentKeys, parent);

        JTable table = new JTable(tableModel);
        table.setRowHeight(table.getRowHeight() + 8);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(360);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);

        KeyStrokeCellRenderer renderer = new KeyStrokeCellRenderer();
        KeyStrokeCellEditor editor = new KeyStrokeCellEditor();
        table.getColumnModel().getColumn(2).setCellRenderer(renderer);
        table.getColumnModel().getColumn(2).setCellEditor(editor);
        table.setPreferredScrollableViewportSize(new Dimension(830, table.getRowHeight() * Math.min(configurable.size(), 16)));

        JTextArea noteArea = new JTextArea(buildNoteText());
        noteArea.setEditable(false);
        noteArea.setOpaque(false);
        noteArea.setFont(UIManager.getFont("Label.font"));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(noteArea, BorderLayout.SOUTH);

        String[] options = {"Save", "Reset to Defaults", "Close"};

        while (true) {
            if (table.isEditing())
                table.getCellEditor().stopCellEditing();

            int choice = JOptionPane.showOptionDialog(parent, panel,
                    "Sharpener Keyboard Shortcuts", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[2]);

            if (table.isEditing())
                table.getCellEditor().stopCellEditing();

            if (choice == 1) { // Reset to Defaults
                int confirm = JOptionPane.showConfirmDialog(parent,
                        "Are you sure you want to reset all shortcuts to their defaults?",
                        "Reset Shortcuts", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    sharedParameters.preferences.safeSetSetting(ShortcutMappings.CUSTOM_SHORTCUTS_SETTING_NAME,
                            null, Preferences.Visibility.GLOBAL);
                    reloadBindings(sharedParameters);
                    show(sharedParameters); // reopen so the table shows the defaults
                }
                return;
            }

            if (choice != 0) // Close or dialog dismissed
                return;

            // Save
            HashMap<String, KeyStroke> keysByAction = new HashMap<>();
            for (int i = 0; i < configurable.size(); i++) {
                keysByAction.put(configurable.get(i).actionKey(), tableModel.keyAt(i));
            }

            List<String> errors = ShortcutMappings.validate(keysByAction);
            if (!errors.isEmpty()) {
                JOptionPane.showMessageDialog(parent, String.join("\n", errors),
                        "Invalid Shortcuts", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            HashMap<String, ArrayList<String>> toSave = new HashMap<>();
            for (ShortcutMappings.ShortcutActionDef def : configurable) {
                KeyStroke keyStroke = keysByAction.get(def.actionKey());
                ArrayList<String> stored = new ArrayList<>();
                if (keyStroke != null)
                    stored.add(ShortcutMappings.toStorageString(keyStroke));
                toSave.put(def.actionKey() + ShortcutMappings.GLOBAL_SCOPE_SUFFIX, stored);
            }

            sharedParameters.preferences.safeSetSetting(ShortcutMappings.CUSTOM_SHORTCUTS_SETTING_NAME,
                    toSave, Preferences.Visibility.GLOBAL);
            reloadBindings(sharedParameters);
            sharedParameters.printDebugMessage("Custom shortcuts have been saved and applied.");
            return;
        }
    }

    private static String buildNoteText() {
        StringBuilder note = new StringBuilder();
        note.append("Click a shortcut cell and press the key combination to set it. ");
        note.append("Press Backspace to clear it, Escape to cancel.\n");
        note.append("A shortcut works anywhere in Burp (even while typing in the editor), so it needs Ctrl, Alt, or a function key.\n");
        note.append("Burp's own hotkeys (Ctrl+R, Ctrl+I, ...) and basic editing keys (Ctrl+C, Ctrl+V, ...) cannot be used.\n\n");

        note.append("Fixed tab-header keys (always on when a tab title has the focus, not editable):\n");
        for (ShortcutMappings.ShortcutActionDef def : ShortcutMappings.getFixedHeaderActionDefinitions()) {
            KeyStroke headerKey = ShortcutMappings.parseKeyStroke(def.fixedHeaderKey());
            note.append("  ").append(ShortcutMappings.formatKeyStroke(headerKey))
                    .append(" = ").append(def.title()).append("\n");
        }

        note.append("\nFixed mouse actions on a tab: Middle-click or Alt+click opens the tab menu, ");
        note.append("Ctrl+Mouse wheel changes the title size.");
        return note.toString();
    }

    // Applies the new shortcuts to the live UI without reloading the extension.
    private static void reloadBindings(ExtensionSharedParameters sharedParameters) {
        try {
            if (sharedParameters.allSettings != null && sharedParameters.allSettings.subTabsSettings != null)
                sharedParameters.allSettings.subTabsSettings.reloadShortcuts();
        } catch (Exception e) {
            sharedParameters.printDebugMessage("Could not reload the sub-tab shortcuts: " + e.getMessage());
        }

        try {
            if (sharedParameters.allSettings != null && sharedParameters.allSettings.burpFrameSettings != null)
                sharedParameters.allSettings.burpFrameSettings.reloadShortcuts();
        } catch (Exception e) {
            sharedParameters.printDebugMessage("Could not reload the Burp frame shortcuts: " + e.getMessage());
        }
    }

    // Table model holding one KeyStroke per configurable action. It validates a captured
    // key and, when the key already belongs to another action, offers to move it.
    private static final class ShortcutTableModel extends AbstractTableModel {
        private final List<ShortcutMappings.ShortcutActionDef> definitions;
        private final KeyStroke[] keys;
        private final Component parent;
        private final String[] columnNames = {"Action", "What it does", "Shortcut (click, then press keys)"};

        private ShortcutTableModel(List<ShortcutMappings.ShortcutActionDef> definitions, KeyStroke[] keys, Component parent) {
            this.definitions = definitions;
            this.keys = keys;
            this.parent = parent;
        }

        private KeyStroke keyAt(int row) {
            return keys[row];
        }

        @Override
        public int getRowCount() {
            return definitions.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 2;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return switch (column) {
                case 0 -> definitions.get(row).title();
                case 1 -> definitions.get(row).description();
                default -> keys[row];
            };
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column != 2)
                return;

            KeyStroke newKey = (KeyStroke) value;
            if (Objects.equals(newKey, keys[row]))
                return;

            if (newKey != null) {
                String reason = ShortcutMappings.rejectReasonForGlobalKey(newKey);
                if (reason != null) {
                    JOptionPane.showMessageDialog(parent, reason, "Invalid Shortcut", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                for (int other = 0; other < keys.length; other++) {
                    if (other != row && newKey.equals(keys[other])) {
                        int response = JOptionPane.showConfirmDialog(parent,
                                ShortcutMappings.formatKeyStroke(newKey) + " is already used by \""
                                        + definitions.get(other).title() + "\".\nMove it to \""
                                        + definitions.get(row).title() + "\"?",
                                "Shortcut Already Used", JOptionPane.YES_NO_OPTION);
                        if (response != JOptionPane.YES_OPTION)
                            return;
                        keys[other] = null;
                        fireTableCellUpdated(other, 2);
                        break;
                    }
                }
            }

            keys[row] = newKey;
            fireTableCellUpdated(row, 2);
        }
    }

    // Shows a keystroke as human readable text, or a hint when none is set.
    private static final class KeyStrokeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            KeyStroke keyStroke = (KeyStroke) value;
            String text = keyStroke == null ? "(none - click and press keys)" : ShortcutMappings.formatKeyStroke(keyStroke);
            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        }
    }

    // Captures a key combination by listening for a real key press, instead of typing text.
    private static final class KeyStrokeCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JLabel label = new JLabel();
        private KeyStroke captured;

        private KeyStrokeCellEditor() {
            label.setOpaque(true);
            label.setFocusable(true);
            label.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Table.selectionBackground"), 2));
            label.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int code = e.getKeyCode();

                    // Escape cancels, Tab leaves the cell for normal table navigation
                    if (code == KeyEvent.VK_ESCAPE) {
                        cancelCellEditing();
                        return;
                    }
                    if (code == KeyEvent.VK_TAB) {
                        cancelCellEditing();
                        return;
                    }

                    e.consume(); // stop the table from acting on the key while capturing

                    // Backspace or Delete clears the shortcut
                    if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
                        captured = null;
                        stopCellEditing();
                        return;
                    }

                    // wait for a real key, not a lone modifier
                    if (isModifierOnly(code))
                        return;

                    int modifiers = e.getModifiersEx()
                            & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK
                            | InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK);
                    captured = KeyStroke.getKeyStroke(code, modifiers);
                    stopCellEditing();
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    e.consume();
                }

                @Override
                public void keyTyped(KeyEvent e) {
                    e.consume();
                }
            });
        }

        private static boolean isModifierOnly(int keyCode) {
            return keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_SHIFT
                    || keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_META
                    || keyCode == KeyEvent.VK_ALT_GRAPH;
        }

        @Override
        public boolean isCellEditable(EventObject event) {
            // start capturing on a single click, not only a double click
            return !(event instanceof MouseEvent mouseEvent) || mouseEvent.getClickCount() >= 1;
        }

        @Override
        public Object getCellEditorValue() {
            return captured;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            captured = (KeyStroke) value;
            label.setText("Press a key combination...");
            label.setBackground(UIManager.getColor("Table.selectionBackground"));
            label.setForeground(UIManager.getColor("Table.selectionForeground"));
            SwingUtilities.invokeLater(label::requestFocusInWindow);
            return label;
        }
    }
}
