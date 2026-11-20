// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.uiSelf.settings;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/** The complete quick-access menu, with the original action paths. EDT only. */
public final class SettingsControls implements AutoCloseable {
    private final SharpenerSettingsActions actions;
    private final JMenu root;
    private final Map<String, AbstractButton> buttons = new LinkedHashMap<>();
    private final Map<String, ButtonGroup> radioGroups = new HashMap<>();
    private final Runnable listener = this::refresh;
    private final MenuListener menuListener = new MenuListener() {
        public void menuSelected(MenuEvent e) { refresh(); }
        public void menuDeselected(MenuEvent e) { }
        public void menuCanceled(MenuEvent e) { }
    };
    private boolean closed;

    public SettingsControls(SharpenerSettingsActions actions, JMenu root) {
        this.actions = actions; this.root = root;
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Settings UI requires EDT");
        root.addMenuListener(menuListener);
        actions.addListener(listener);
        try { refresh(); } catch (RuntimeException | LinkageError failure) { close(); throw failure; }
    }
    public void refresh() {
        if (closed) return;
        List<SharpenerSettingsActions.Control> controls = actions.controls();
        if (!buttons.keySet().equals(new LinkedHashSet<>(controls.stream().map(SharpenerSettingsActions.Control::path).toList()))) build(controls);
        radioGroups.values().forEach(ButtonGroup::clearSelection);
        for (var control : controls) {
            AbstractButton button = buttons.get(control.path());
            button.setSelected(control.selected().getAsBoolean()); button.setIcon(control.icon());
        }
    }
    private void build(List<SharpenerSettingsActions.Control> controls) {
        root.removeAll(); buttons.clear(); radioGroups.clear();
        Map<String, JComponent> groups = new LinkedHashMap<>(); groups.put("", root);
        for (var control : controls) {
            String path = control.path();
            String parent = control.parentPath();
            JComponent group = group(parent, groups);
            AbstractButton button = switch (control.kind()) {
                case CHECK -> new JCheckBoxMenuItem(control.label());
                case RADIO -> new JRadioButtonMenuItem(control.label());
                case ACTION -> new JMenuItem(control.label());
            };
            button.setName(path); button.setToolTipText(control.description().isEmpty() ? null : control.description());
            button.getAccessibleContext().setAccessibleDescription(control.description());
            if (control.kind() == SharpenerSettingsActions.Kind.RADIO) radioGroups.computeIfAbsent(parent, k -> new ButtonGroup()).add(button);
            button.addActionListener(e -> { actions.dialogs.setAnchor(button); actions.invoke(control, button.isSelected()); refresh(); });
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            group.add(button); buttons.put(path, button);
        }
        root.revalidate(); root.repaint();
    }
    private JComponent group(String path, Map<String, JComponent> groups) {
        if (groups.containsKey(path)) return groups.get(path);
        int slash = path.lastIndexOf('/');
        String parent = slash < 0 ? "" : path.substring(0, slash);
        String label = path.substring(slash + 1);
        JComponent container = group(parent, groups);
        JComponent created = new JMenu(label);
        container.add(created); groups.put(path, created); return created;
    }
    @Override public void close() {
        if (closed) return;
        closed = true; actions.removeListener(listener);
        root.removeMenuListener(menuListener);
        for (AbstractButton button : buttons.values()) for (var l : button.getActionListeners()) button.removeActionListener(l);
        buttons.clear(); radioGroups.clear(); root.removeAll();
    }
}
