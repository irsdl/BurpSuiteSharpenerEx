// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.MouseAdapterExtensionHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.function.Consumer;

public class SubTabsListenersV2 {
    private final Consumer<MouseEvent> mouseEventConsumer;
    private final ExtensionSharedParameters sharedParameters;
    private boolean _isUpdateInProgress = false;
    private ArrayList<BurpUITools.MainTabs> accessibleTabs;
    private final boolean _isShortcutEnabled = true;
    public HashMap<String, String> subTabsShortcutMappings = new HashMap<>() {{
        put("control ENTER", "ShowMenu");
        put("control shift ENTER", "ShowMenu");
        put("DOWN", "ShowMenu");
        put("control shift F", "FindTabs");
        put("F3", "NextFind");
        put("control F3", "NextFind");
        put("shift F3", "PreviousFind");
        put("control shift F3", "PreviousFind");
        put("HOME", "FirstTab");
        put("END", "LastTab");
        put("control shift HOME", "FirstTab");
        put("control shift END", "LastTab");
        put("LEFT", "PreviousTab");
        put("RIGHT", "NextTab");
        put("control shift LEFT", "PreviousTab");
        put("control shift RIGHT", "NextTab");
        put("alt LEFT", "PreviouslySelectedTab");
        put("alt RIGHT", "NextlySelectedTab");
        put("control alt LEFT", "PreviouslySelectedTab");
        put("control alt RIGHT", "NextlySelectedTab");
        put("control C", "CopyTitle");
        put("control shift C", "CopyTitle");
        put("control V", "PasteTitle");
        put("control shift V", "PasteTitle");
        put("F2", "RenameTitle");
        put("control F2", "RenameTitle");
    }};

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
        addTabListener();
    }

    public void addTabListener() {
        sharedParameters.printDebugMessage("addSubTabListener");
        accessibleTabs = new ArrayList<>();

        for (BurpUITools.MainTabs supportedTab : sharedParameters.getAllSubTabSupportedTabs()) {
            addListenerToSupportedTabbedPanels(supportedTab);
        }
    }

    public void removeTabListener() {
        sharedParameters.printDebugMessage("removeSubTabListener");
        accessibleTabs = new ArrayList<>();

        for (BurpUITools.MainTabs supportedTab : sharedParameters.getAllSubTabSupportedTabs()) {
            removeListenerFromTabbedPanels(supportedTab);
        }

    }

    private void addListenerToSupportedTabbedPanels(BurpUITools.MainTabs toolName) {
        var currentToolTabbedPane = sharedParameters.get_toolTabbedPane(toolName);
        if (currentToolTabbedPane == null) {
            sharedParameters.printDebugMessage("addListenerToSupportedTabbedPanels: Listener could not be added for " + toolName.toString());
            return;
        }

        sharedParameters.printDebugMessage("Adding listener to " + toolName);

        accessibleTabs.add(toolName);


        // this is a dirty hack to keep the colours as they go black after drag and drop!
        // this also makes sure we always have the latest version of the tabs saved in the variables after add/remove
        // this is enough for repeater but Intruder changes the colour, so it should be higher
        PropertyChangeListener tabPropertyChangeListener = evt -> {
            if (!get_isUpdateInProgress() && evt.getPropertyName().equalsIgnoreCase("indexForTabComponent")) {
                // this is a dirty hack to keep the colours as they change after drag and drop!
                // this also makes sure we always have the latest version of the tabs saved in the variables after add/remove
                // this is in charge of adding the right click menu to the new tabs by doing this
                set_isUpdateInProgress(true);

                int delay = 3000; // this is enough for repeater but Intruder changes the colour, so it should be higher
                if (toolName.equals(BurpUITools.MainTabs.Intruder)) {
                    delay = 10000;
                }

                sharedParameters.delayedTasks.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                if (sharedParameters.isUnloaded())
                                    return;
                                SwingUtilities.invokeLater(() -> {
                                    if (sharedParameters.isUnloaded())
                                        return;
                                    set_isUpdateInProgress(true);
                                    sharedParameters.allSettings.subTabsSettings.loadSettings(toolName);
                                    sharedParameters.allSettings.subTabsSettings.saveSettings(toolName);
                                    set_isUpdateInProgress(false);
                                });
                            }
                        },
                        delay
                );
            }
        };

        // Loading all the tabs

        currentToolTabbedPane.addPropertyChangeListener("indexForTabComponent", tabPropertyChangeListener);

        // when Burp Suite is loaded for the first time, Repeater and Intruder tabs are empty in the latest versions rather than having one tab
        // This is to address the issue of component change when the first tab is being created
        currentToolTabbedPane.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                sharedParameters.delayedTasks.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                if (sharedParameters.isUnloaded())
                                    return;
                                SwingUtilities.invokeLater(() -> {
                                    if (sharedParameters.isUnloaded())
                                        return;
                                    // This will be triggered when Burp creates items in Repeater or Intruder
                                    BurpUITools.MainTabs currentToolName = BurpUITools.getMainTabsObjFromString(sharedParameters.get_rootTabbedPaneUsingMontoya().getTitleAt(sharedParameters.get_rootTabbedPaneUsingMontoya().indexOfComponent(((Component) e.getSource()).getParent())));
                                    var currentToolTabbedPane = sharedParameters.get_toolTabbedPane(currentToolName);
                                    if (currentToolTabbedPane != null) {
                                        // the listener is registered under the "indexForTabComponent" property, so the guard must query the same name
                                        if (currentToolTabbedPane.getPropertyChangeListeners("indexForTabComponent").length == 0)
                                            currentToolTabbedPane.addPropertyChangeListener("indexForTabComponent", tabPropertyChangeListener);

                                        addSubTabsListener(currentToolName);

                                        // as there is no other getComponentListeners by default, we can remove them all
                                        for (ComponentListener cl : currentToolTabbedPane.getComponentListeners()) {
                                            currentToolTabbedPane.removeComponentListener(cl);
                                        }

                                        sharedParameters.allSettings.subTabsSettings.loadSettings(currentToolName);
                                        sharedParameters.allSettings.subTabsSettings.saveSettings(currentToolName);
                                        set_isUpdateInProgress(false);
                                    }
                                });
                            }
                        },
                        5000 // 5 seconds delay just in case Burp is very slow on the device
                );
            }
        });

        addSubTabsListener(toolName);
    }


    private void addSubTabsListener(BurpUITools.MainTabs toolName) {
        var toolTabbedPane = sharedParameters.get_toolTabbedPane(toolName);
        if (toolTabbedPane == null) {
            sharedParameters.printDebugMessage("addSubTabsListener: Listener could not be added for " + toolName.toString());
            return;
        }
        if (sharedParameters.allSubTabContainerHandlers.get(toolName) == null ||
                (sharedParameters.allSubTabContainerHandlers.get(toolName).size() != toolTabbedPane.getTabCount() - 1 && !sharedParameters.isTabGroupSupportedByDefault)) {
            ArrayList<SubTabsContainerHandler> subTabsContainerHandlers = new ArrayList<>();
            for (int subTabIndex = 0; subTabIndex < toolTabbedPane.getTabCount(); subTabIndex++) {
                SubTabsContainerHandler subTabsContainerHandler = new SubTabsContainerHandler(sharedParameters, toolTabbedPane, subTabIndex, false);
                subTabsContainerHandlers.add(subTabsContainerHandler);
            }

            sharedParameters.allSubTabContainerHandlers.put(toolName, subTabsContainerHandlers);
        }

        toolTabbedPane.addMouseListener(new MouseAdapterExtensionHandler(mouseEventConsumer, MouseEvent.MOUSE_RELEASED));

        //Defining shortcuts for search in tab titles: ctrl+shift+f , F3, shift+F3
        if (_isShortcutEnabled) {
            clearInputMap(toolTabbedPane);
            subTabsShortcutMappings.forEach((k, v) -> toolTabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(k), v));

            toolTabbedPane.getActionMap().put("ShowMenu", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.showPopupMenu(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("FindTabs", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.defineRegExPopupForSearchAndJump(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("NextFind", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.searchInTabTitlesAndJump(sharedParameters, e, true);
                }
            });
            toolTabbedPane.getActionMap().put("PreviousFind", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.searchInTabTitlesAndJump(sharedParameters, e, false);
                }
            });
            toolTabbedPane.getActionMap().put("FirstTab", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.jumpToFirstTab(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("LastTab", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.jumpToLastTab(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("PreviousTab", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.jumpToPreviousTab(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("NextTab", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.jumpToNextTab(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("PreviouslySelectedTab", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.jumpToPreviouslySelectedTab(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("NextlySelectedTab", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.jumpToNextlySelectedTab(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("CopyTitle", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.copyTitle(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("PasteTitle", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.pasteTitle(sharedParameters, e);
                }
            });
            toolTabbedPane.getActionMap().put("RenameTitle", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SubTabsActions.renameTitle(sharedParameters, e);
                }
            });
        }
        //tabComponent.removeMouseListener(tabComponent.getMouseListeners()[1]); --> this will remove the current right click menu!

        sharedParameters.printDebugMessage("Menu has now been loaded for " + toolName.toString());
    }

    private void removeListenerFromTabbedPanels(BurpUITools.MainTabs toolName) {
        var currentToolTabbedPane = sharedParameters.get_toolTabbedPane(toolName);
        if (currentToolTabbedPane == null) {
            sharedParameters.printDebugMessage("removeListenerFromTabbedPanels: Listener could not be removed for " + toolName.toString());
            return;
        }

        if (!sharedParameters.getAllSubTabSupportedTabs().contains(toolName)) return;

        accessibleTabs.add(toolName);

        // as there is no other PropertyChangeListener with propertyName of "indexForTabComponent" by default, we can remove them all
        PropertyChangeListener[] pclArray = currentToolTabbedPane.getPropertyChangeListeners("indexForTabComponent");
        for (PropertyChangeListener pcl : pclArray) {
            currentToolTabbedPane.removePropertyChangeListener("indexForTabComponent", pcl);
        }

        // as there is no other getComponentListeners by default, we can remove them all
        // this prevents the tab-change component listener from accumulating on every reload
        for (ComponentListener cl : currentToolTabbedPane.getComponentListeners()) {
            currentToolTabbedPane.removeComponentListener(cl);
        }

        for (MouseListener mouseListener : currentToolTabbedPane.getMouseListeners()) {
            if (mouseListener instanceof MouseAdapterExtensionHandler) {
                currentToolTabbedPane.removeMouseListener(mouseListener);
            }
        }

        // as there is no other getKeyListeners by default, we can remove them all
        for (KeyListener keyListener : currentToolTabbedPane.getKeyListeners()) {
            currentToolTabbedPane.removeKeyListener(keyListener);
        }

        // There is no bindings on these items, so it can be cleared
        if (_isShortcutEnabled) {
            clearInputMap(currentToolTabbedPane);
        }
    }

    private void clearInputMap(JComponent jc) {
        subTabsShortcutMappings.forEach((k, v) -> jc.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(k), "none"));
    }

    private void set_isUpdateInProgress(boolean _isUpdateInProgress) {
        this._isUpdateInProgress = _isUpdateInProgress;
    }

    private boolean get_isUpdateInProgress() {
        return this._isUpdateInProgress;
    }
}
