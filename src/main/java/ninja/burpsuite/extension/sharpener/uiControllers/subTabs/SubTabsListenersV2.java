// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.uiControllers.shortcuts.ShortcutMappings;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.MouseAdapterExtensionHandler;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;

public final class SubTabsListenersV2 {
    private final Consumer<MouseEvent> mouseEventConsumer;
    private final ExtensionSharedParameters sharedParameters;
    private final java.util.Map<JTabbedPane, Runnable> listenerCleanup = new java.util.IdentityHashMap<>();
    private final boolean _isShortcutEnabled = true;

    // Records every tab selection change in the navigation history, including
    // selections made by Burp itself or by native keyboard navigation.
    // Sharpener jumps are skipped because they update the history themselves.
    private final class TabSelectionHistoryRecorder implements ChangeListener {
        private final BurpUITools.MainTabs toolName;

        private TabSelectionHistoryRecorder(BurpUITools.MainTabs toolName) {
            this.toolName = toolName;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (SubTabsActions.isTabJumpInProgress())
                return;

            if (!(e.getSource() instanceof JTabbedPane tabbedPane))
                return;

            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex < 0)
                return;

            LinkedList<Integer> previousHistory = sharedParameters.subTabPreviouslySelectedIndexHistory.get(toolName);
            LinkedList<Integer> nextHistory = sharedParameters.subTabNextlySelectedIndexHistory.get(toolName);
            if (previousHistory == null || nextHistory == null)
                return;

            boolean isLastTabIndex = selectedIndex == tabbedPane.getTabCount() - 1;
            if (isLastTabIndex && !sharedParameters.isTabGroupSupportedByDefault)
                return; // the "..." tab only creates a new tab, it is not a real selection

            ShortcutMappings.recordSelectionHistory(previousHistory, selectedIndex,
                    isLastTabIndex, sharedParameters.isTabGroupSupportedByDefault);
            // a manual tab change invalidates the forward history
            nextHistory.clear();
        }
    }

    public SubTabsListenersV2(ExtensionSharedParameters sharedParameters, Consumer<MouseEvent> mouseEventConsumer) {
        this.sharedParameters = sharedParameters;
        this.mouseEventConsumer = mouseEventConsumer;
        refreshListeners();
    }

    // Removes and re-adds the tab listeners on the same object.
    // This is reused on later loadSettings calls so a new listener object is not
    // created every time, which used to leave stale component listeners behind.
    public void refreshListeners() {
        removeTabListener();
        if (!sharedParameters.isUnloaded()) addTabListener();
    }

    public void addTabListener() {
        for (BurpUITools.MainTabs tool : sharedParameters.getAllSubTabSupportedTabs()) {
            JTabbedPane pane = sharedParameters.get_toolTabbedPane(tool);
            if (pane == null || listenerCleanup.containsKey(pane)) continue;
            sharedParameters.runOptionalFeature(tool + " tab listeners", () -> attach(pane, tool));
        }
    }

    private void requestRescan(BurpUITools.MainTabs tool) {
        if (sharedParameters.isUnloaded() || sharedParameters.allSettings == null
                || sharedParameters.allSettings.subTabsSettings == null) return;
        sharedParameters.allSettings.subTabsSettings.scheduleTabRescan(tool);
    }

    private void attach(JTabbedPane pane, BurpUITools.MainTabs tool) {
        PropertyChangeListener property = event -> {
            if ("indexForTabComponent".equals(event.getPropertyName())) requestRescan(tool);
        };
        // Container events are a public Swing fallback if Burp changes its header property events.
        ContainerListener container = new ContainerAdapter() {
            @Override public void componentAdded(ContainerEvent event) { requestRescan(tool); }
            @Override public void componentRemoved(ContainerEvent event) { requestRescan(tool); }
        };
        ComponentListener component = new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent event) { requestRescan(tool); }
            @Override public void componentHidden(ComponentEvent event) { requestRescan(tool); }
        };
        MouseListener mouse = new MouseAdapterExtensionHandler(mouseEventConsumer, MouseEvent.MOUSE_RELEASED);
        ChangeListener history = new TabSelectionHistoryRecorder(tool);
        // Keep the actual pane and exact listener identities, including after Burp replaces it.
        listenerCleanup.put(pane, () -> {
            pane.removePropertyChangeListener("indexForTabComponent", property);
            pane.removeContainerListener(container);
            pane.removeComponentListener(component);
            pane.removeMouseListener(mouse);
            pane.removeChangeListener(history);
            if (_isShortcutEnabled) ShortcutMappings.uninstallFromComponent(pane);
        });
        pane.addPropertyChangeListener("indexForTabComponent", property);
        pane.addContainerListener(container);
        pane.addComponentListener(component);
        pane.addMouseListener(mouse);
        pane.addChangeListener(history);
        if (_isShortcutEnabled)
            ShortcutMappings.installOnTabbedPane(pane,
                    ShortcutMappings.getSavedOverrides(sharedParameters), buildShortcutHandlers());
    }

    public void removeTabListener() {
        for (Runnable cleanup : listenerCleanup.values())
            sharedParameters.runOptionalFeature("tab listener cleanup", cleanup);
        listenerCleanup.clear();
    }

    // Maps every shortcut action key to its handler. Used by ShortcutMappings when
    // the key bindings are installed.
    private HashMap<String, Consumer<ActionEvent>> buildShortcutHandlers() {
        HashMap<String, Consumer<ActionEvent>> handlers = new HashMap<>();
        String prefix = ShortcutMappings.ACTION_KEY_PREFIX;
        handlers.put(prefix + "ShowMenu", e -> SubTabsActions.showPopupMenu(sharedParameters, e));
        handlers.put(prefix + "FindTabs", e -> SubTabsActions.defineRegExPopupForSearchAndJump(sharedParameters, e));
        handlers.put(prefix + "NextFind", e -> SubTabsActions.searchInTabTitlesAndJump(sharedParameters, e, true));
        handlers.put(prefix + "PreviousFind", e -> SubTabsActions.searchInTabTitlesAndJump(sharedParameters, e, false));
        handlers.put(prefix + "FirstTab", e -> SubTabsActions.jumpToFirstTab(sharedParameters, e));
        handlers.put(prefix + "LastTab", e -> SubTabsActions.jumpToLastTab(sharedParameters, e));
        handlers.put(prefix + "PreviousTab", e -> SubTabsActions.jumpToPreviousTab(sharedParameters, e));
        handlers.put(prefix + "NextTab", e -> SubTabsActions.jumpToNextTab(sharedParameters, e));
        handlers.put(prefix + "PreviouslySelectedTab", e -> SubTabsActions.jumpToPreviouslySelectedTab(sharedParameters, e));
        handlers.put(prefix + "NextlySelectedTab", e -> SubTabsActions.jumpToNextlySelectedTab(sharedParameters, e));
        handlers.put(prefix + "CopyTitle", e -> SubTabsActions.copyTitle(sharedParameters, e));
        handlers.put(prefix + "PasteTitle", e -> SubTabsActions.pasteTitle(sharedParameters, e));
        handlers.put(prefix + "RenameTitle", e -> SubTabsActions.renameTitle(sharedParameters, e));
        handlers.put(prefix + "FocusRequestEditor", e -> SubTabsActions.focusRequestEditor(sharedParameters, e));
        handlers.put(prefix + "FocusTab", e -> SubTabsActions.focusTab(sharedParameters, e));
        return handlers;
    }

}
