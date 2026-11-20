// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.uiSelf.settings;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/** A settings form, independent of the menu hierarchy. All changes still use the shared actions. */
public final class SettingsPanelView implements AutoCloseable {
    private static final String STYLE = "Global Settings/Tools' Template And Style/";
    public record Option(String path, String label, Icon icon) {
        @Override public String toString() { return label; }
    }
    private final ExtensionSharedParameters p;
    private final SharpenerSettingsActions actions;
    private final JPanel root;
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<String, AbstractButton> buttons = new LinkedHashMap<>();
    private final Map<String, JComboBox<Option>> selectors = new LinkedHashMap<>();
    private final List<Runnable> refreshValues = new ArrayList<>();
    private final Runnable listener = this::refresh;
    private Map<String, SharpenerSettingsActions.Control> controls = new LinkedHashMap<>();
    private boolean refreshing;
    private boolean closed;

    public SettingsPanelView(ExtensionSharedParameters p, JPanel root) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Settings UI requires EDT");
        this.p = p; this.actions = p.settingsActions(); this.root = root;
        root.setLayout(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        root.add(note("Changes apply immediately. Your settings are also available from the Sharpener menu."), BorderLayout.NORTH);
        tabs.setName("Sharpener settings scope");
        root.add(tabs, BorderLayout.CENTER);
        controls = snapshot();
        try { build(); actions.addListener(listener); refresh(); }
        catch (RuntimeException | LinkageError failure) { close(); throw failure; }
    }
    private Map<String, SharpenerSettingsActions.Control> snapshot() {
        Map<String, SharpenerSettingsActions.Control> result = new LinkedHashMap<>();
        for (var control : actions.controls()) result.put(control.path(), control);
        return result;
    }
    private void build() {
        Page global = page("Global", "Applies across projects on this installation.");
        Page project = page("Project", "Title and window icon are saved with this project.");

        JPanel appearance = card(global, "Tool appearance", "Choose icons and the tool tabs that use them.");
        JPanel appearanceForm = form();
        selector(appearanceForm, "Icon theme", STYLE + "Icons' Theme/", true);
        JTextField directory = valueField();
        row(appearanceForm, "Custom folder", directory);
        refreshValues.add(() -> { directory.setText(p.preferences.safeGetStringSetting("ToolsThemeCustomPath")); directory.setEnabled(p.preferences.safeGetStringSetting("ToolsThemeName").equalsIgnoreCase("custom")); });
        selector(appearanceForm, "Icon size", STYLE + "Icons' Size/", false);
        appearance.add(appearanceForm);
        JPanel commands = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); commands.setOpaque(false);
        commands.add(button(STYLE + "Enable All", "Enable all")); commands.add(button(STYLE + "Disable All", "Disable all"));
        commands.setAlignmentX(Component.LEFT_ALIGNMENT); appearance.add(Box.createVerticalStrut(12)); appearance.add(commands);
        JPanel tools = new JPanel(new GridLayout(0, 2, 12, 3)) {
            @Override public void doLayout() {
                ((GridLayout)getLayout()).setColumns(getWidth() >= 600 ? 3 : getWidth() >= 340 ? 2 : 1);
                super.doLayout();
            }
        };
        tools.setOpaque(false); tools.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (var control : controls.values()) if (control.path().startsWith(STYLE) && control.kind() == SharpenerSettingsActions.Kind.CHECK)
            tools.add(checkbox(control.path(), control.label()));
        appearance.add(Box.createVerticalStrut(8)); appearance.add(tools); appearance.add(Box.createVerticalStrut(8));
        appearance.add(checkbox("Global Settings/Scrollable Tool Pane", "Use a scrollable row of tool tabs"));

        JPanel window = card(global, "Window behavior", "Control how Burp restores its window.");
        window.add(checkbox("Global Settings/Use Last Screen Position And Size When Opened", "Remember window position and size"));
        window.add(checkbox("Global Settings/Detect Off Screen Window Position", "Recover windows that are off screen"));

        JPanel capabilities = card(global, "Traffic highlighting", "Enable Sharpener's highlighting features.");
        for (var control : controls.values()) if (control.path().startsWith("Global Settings/Supported Capabilities/")) {
            String label = switch (control.label()) {
                case "Manual Highlighter (Proxy and Logger)" -> "Manual highlighting in Proxy and Logger";
                case "PwnFox Highlighter" -> "PwnFox highlighting";
                case "PwnFox: Remove the color header" -> "Remove the X-PwnFox-Color request header";
                default -> control.label();
            };
            JCheckBox check = checkbox(control.path(), label);
            if (control.label().startsWith("PwnFox:")) check.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
            capabilities.add(check);
        }
        JPanel diagnostics = card(global, "Diagnostics and updates", "Update checks are optional and contact the project on GitHub.");
        JPanel debugForm = form(); selector(debugForm, "Debug logging", "Global Settings/Debug Settings/", false); diagnostics.add(debugForm);
        diagnostics.add(Box.createVerticalStrut(8)); diagnostics.add(checkbox("Check for Update on Start", "Check for updates when Sharpener starts"));
        diagnostics.add(note("Enabling update checks also checks for an update now."));
        JPanel shortcuts = card(global, "Keyboard shortcuts", "View, edit and reset Sharpener's shortcuts.");
        shortcuts.add(button("Keyboard Shortcuts", "Edit keyboard shortcuts..."));

        JPanel projectAppearance = card(project, "Window appearance", "Customize the title and icon for this project.");
        JPanel projectForm = form();
        JTextField title = valueField();
        JPanel titleInput = new JPanel(new BorderLayout(8, 0)); titleInput.setOpaque(false); titleInput.add(title, BorderLayout.CENTER);
        JPanel titleCommands = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); titleCommands.setOpaque(false);
        titleCommands.add(button("Project Settings/Change Burp Suite Title", "Change...")); titleCommands.add(button("Project Settings/Reset Burp Suite Title", "Reset"));
        titleInput.add(titleCommands, BorderLayout.EAST); row(projectForm, "Window title", titleInput);
        refreshValues.add(() -> {
            String saved = p.preferences.safeGetStringSetting("BurpTitle");
            JFrame frame = p.get_mainFrameUsingMontoya();
            title.setText(saved.isBlank() ? frame == null ? "Default project title" : frame.getTitle() : saved);
        });
        selector(projectForm, "Window icon", "Project Settings/Change Burp Suite Icon/", false);
        JTextField customIcon = valueField(); row(projectForm, "Custom image", customIcon);
        refreshValues.add(() -> customIcon.setText(p.preferences.safeGetStringSetting("BurpIconCustomPath")));
        projectAppearance.add(projectForm); projectAppearance.add(Box.createVerticalStrut(12));
        projectAppearance.add(button("Project Settings/Reset Burp Suite Icon", "Reset window icon"));
        global.finish(); project.finish();
    }
    private Page page(String title, String description) {
        Page page = new Page();
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 14, 0); page.add(note(description), c);
        JScrollPane scroll = new JScrollPane(page, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getVerticalScrollBar().setUnitIncrement(24);
        tabs.addTab(title, scroll); return page;
    }
    private JPanel card(Page page, String heading, String description) {
        JPanel card = new JPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        Color border = UIManager.getColor("Component.borderColor");
        if (border == null) border = UIManager.getColor("Separator.foreground");
        if (border == null) border = UIManager.getColor("Label.disabledForeground");
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border), BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel title = new JLabel(heading); title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1));
        title.setAlignmentX(Component.LEFT_ALIGNMENT); card.add(title); card.add(Box.createVerticalStrut(4)); card.add(note(description)); card.add(Box.createVerticalStrut(12));
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = page.getComponentCount(); c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.NORTHWEST; c.insets = new Insets(0, 0, 12, 0);
        page.add(card, c); return card;
    }
    private static JPanel form() {
        JPanel form = new JPanel(new GridBagLayout()); form.setOpaque(false); form.setAlignmentX(Component.LEFT_ALIGNMENT); return form;
    }
    private static void row(JPanel form, String label, JComponent field) {
        int index = form.getComponentCount() / 2;
        JLabel caption = new JLabel(label); caption.setLabelFor(field);
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = index; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(5, 0, 5, 14);
        form.add(caption, c); c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(5, 0, 5, 0);
        field.setMinimumSize(new Dimension(80, field.getPreferredSize().height)); form.add(field, c);
    }
    private static JTextField valueField() {
        JTextField value = new JTextField(); value.setEditable(false); value.setColumns(12); return value;
    }
    private static JComponent note(String text) {
        JTextArea note = new JTextArea(text) {
            @Override public Dimension getPreferredSize() {
                FontMetrics metrics = getFontMetrics(getFont());
                int available = getWidth() > 0 ? getWidth() : 480;
                int lines = 1, used = 0;
                for (String word : getText().split(" ")) {
                    int size = metrics.stringWidth(word + " ");
                    if (used > 0 && used + size > available) { lines++; used = 0; }
                    used += size;
                }
                return new Dimension(Math.min(480, metrics.stringWidth(getText())), lines * metrics.getHeight() + 2);
            }
            @Override public Dimension getMinimumSize() { return new Dimension(0, getFontMetrics(getFont()).getHeight() + 2); }
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        note.setLineWrap(true); note.setWrapStyleWord(true); note.setFont(UIManager.getFont("Label.font"));
        Color foreground = UIManager.getColor("Label.disabledForeground"); if (foreground != null) note.setForeground(foreground);
        note.setAlignmentX(Component.LEFT_ALIGNMENT); note.setBorder(BorderFactory.createEmptyBorder()); return note;
    }
    private JButton button(String path, String label) {
        JButton button = new JButton(label); bind(button, path); return button;
    }
    private JCheckBox checkbox(String path, String label) {
        JCheckBox check = new JCheckBox(label); bind(check, path); return check;
    }
    private void bind(AbstractButton button, String path) {
        button.setName(path); button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setToolTipText(controls.get(path).description());
        button.addActionListener(e -> { if (!refreshing) invoke(path, button, button.isSelected()); }); buttons.put(path, button);
    }
    private void selector(JPanel form, String label, String prefix, boolean themes) {
        JComboBox<Option> combo = new JComboBox<>(); combo.setName(prefix); combo.setMaximumRowCount(12);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel rendered = (JLabel)super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Option option) rendered.setIcon(option.icon()); return rendered;
            }
        });
        combo.putClientProperty("Sharpener.settings.themes", themes);
        combo.addActionListener(e -> { if (!refreshing && combo.getSelectedItem() instanceof Option option && !option.path().isEmpty()) invoke(option.path(), combo, true); });
        selectors.put(prefix, combo); row(form, label, combo);
    }
    private void invoke(String path, JComponent component, boolean selected) {
        if (closed) return;
        var control = controls.get(path); if (control == null) return;
        actions.dialogs.setAnchor(component);
        try { actions.invoke(control, selected); } finally { refresh(); }
    }
    public void refresh() {
        if (closed) return;
        refreshing = true;
        try {
            controls = snapshot();
            buttons.forEach((path, button) -> button.setSelected(controls.get(path).selected().getAsBoolean()));
            selectors.forEach((prefix, combo) -> {
                List<Option> options = new ArrayList<>(); options.add(new Option("", "Choose an icon", null));
                for (var control : controls.values()) if (control.path().startsWith(prefix)) {
                    String label = control.label();
                    if (Boolean.TRUE.equals(combo.getClientProperty("Sharpener.settings.themes"))) label = switch (label) {
                        case "none" -> "No theme"; case "custom directory" -> "Custom directory..."; case "@irsdl" -> "Sharpener (@irsdl)";
                        default -> Character.toUpperCase(label.charAt(0)) + label.substring(1);
                    };
                    else if (prefix.contains("Icons' Size")) label += " px";
                    options.add(new Option(control.path(), label, control.icon()));
                }
                if (!prefix.startsWith("Project Settings/")) options.removeFirst();
                boolean same = combo.getItemCount() == options.size();
                for (int i = 0; same && i < options.size(); i++) same = combo.getItemAt(i).equals(options.get(i));
                if (!same) combo.setModel(new DefaultComboBoxModel<>(options.toArray(Option[]::new)));
                int selected = -1;
                for (int i = 0; i < options.size(); i++) { var control = controls.get(options.get(i).path()); if (control != null && control.selected().getAsBoolean()) { selected = i; break; } }
                combo.setSelectedIndex(selected < 0 && prefix.startsWith("Project Settings/") ? 0 : selected);
            });
            refreshValues.forEach(Runnable::run);
        } finally { refreshing = false; }
    }
    @Override public void close() {
        if (closed) return;
        closed = true; actions.removeListener(listener);
        for (var button : buttons.values()) for (var l : button.getActionListeners()) button.removeActionListener(l);
        for (var combo : selectors.values()) for (var l : combo.getActionListeners()) combo.removeActionListener(l);
        root.removeAll(); controls.clear(); buttons.clear(); selectors.clear(); refreshValues.clear();
    }
    private static final class Page extends JPanel implements Scrollable {
        Page() { super(new GridBagLayout()); setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 8)); }
        void finish() { GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = getComponentCount(); c.weighty = 1; c.fill = GridBagConstraints.VERTICAL; add(Box.createVerticalGlue(), c); }
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(620, 600); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(24, r.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
