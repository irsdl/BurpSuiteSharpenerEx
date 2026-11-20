// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.uiSelf.settings;

import burp.api.montoya.ui.settings.SettingsPanel;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import javax.swing.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Set;

/** Loaded only by the optional native integration, after capability detection. */
public final class SharpenerSettingsPanel implements SettingsPanel, AutoCloseable {
    private final JPanel component = new JPanel();
    private final SettingsPanelView controls;
    private final HierarchyListener shown = e -> {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && component.isShowing()) refresh();
    };
    public SharpenerSettingsPanel(ExtensionSharedParameters p) {
        controls = new SettingsPanelView(p, component);
        component.setName("Sharpener Settings");
        component.addHierarchyListener(shown);
        try { p.montoyaApi.userInterface().applyThemeToComponent(component); }
        catch (RuntimeException | LinkageError failure) { close(); throw failure; }
    }
    private void refresh() { controls.refresh(); }
    @Override public JComponent uiComponent() { return component; }
    @Override public Set<String> keywords() { return Set.of("Sharpener", "icons", "themes", "highlights", "shortcuts", "project title", "PwnFox", "global", "updates"); }
    @Override public void close() { component.removeHierarchyListener(shown); controls.close(); }
}
