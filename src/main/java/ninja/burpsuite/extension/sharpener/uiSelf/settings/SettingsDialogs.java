// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.uiSelf.settings;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/** Own only our dialogs, including dialogs opened above Burp's modal Settings window. EDT only. */
public final class SettingsDialogs implements AutoCloseable {
    private final ExtensionSharedParameters p;
    private final Set<JDialog> dialogs = new HashSet<>();
    private Component anchor;
    private boolean closed;

    SettingsDialogs(ExtensionSharedParameters p) { this.p = p; }
    public void setAnchor(Component component) { anchor = component; }
    public Component parent() {
        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        JFrame frame = p.get_mainFrameUsingMontoya();
        if (anchor != null && anchor.isShowing()) {
            Window owner = SwingUtilities.getWindowAncestor(anchor);
            for (Window w = active; w != null; w = w.getOwner()) if (w == owner) return active;
            return anchor;
        }
        for (Window w = active; w != null; w = w.getOwner()) if (w == frame) return active;
        return frame;
    }
    public int option(Object message, String title, String[] options, int initial) {
        if (closed || p.isUnloaded() || parent() == null) return -1;
        JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, options[initial]);
        JDialog dialog = pane.createDialog(parent(), title);
        show(dialog);
        Object value = pane.getValue();
        for (int i = 0; i < options.length; i++) if (options[i].equals(value)) return i;
        return -1;
    }
    public boolean confirm(String title, String message) {
        return option(message, title, new String[]{"Yes", "No"}, 0) == 0;
    }
    public String titleInput(String current) {
        JTextField input = new JTextField(current, 35);
        SwingUtilities.invokeLater(() -> { if (!closed && !p.isUnloaded()) { input.requestFocusInWindow(); input.selectAll(); } });
        return option(new Object[]{"Change Burp Suite Title String To:", input},
                "Change Burp Suite Title", new String[]{"OK", "Cancel"}, 0) == 0 ? input.getText() : null;
    }
    public String choose(String initial, boolean directory, FileFilter filter) {
        if (closed || p.isUnloaded() || parent() == null) return null;
        JFileChooser chooser = new JFileChooser();
        if (directory) chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (filter != null) chooser.setFileFilter(filter);
        if (initial != null && !initial.isBlank()) chooser.setCurrentDirectory(new File(initial));
        Window owner = parent() instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent());
        JDialog dialog = new JDialog(owner, "Open", Dialog.ModalityType.DOCUMENT_MODAL);
        String[] result = {null};
        chooser.addActionListener(e -> {
            if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand()) && chooser.getSelectedFile() != null)
                result[0] = chooser.getSelectedFile().getAbsolutePath();
            dialog.dispose();
        });
        dialog.add(chooser); dialog.pack(); dialog.setLocationRelativeTo(parent()); show(dialog);
        return result[0];
    }
    private void show(JDialog dialog) {
        dialogs.add(dialog);
        try { dialog.setVisible(true); } finally { dialogs.remove(dialog); dialog.dispose(); }
    }
    @Override public void close() {
        closed = true;
        for (JDialog dialog : Set.copyOf(dialogs)) dialog.dispose();
        dialogs.clear(); anchor = null;
    }
}
