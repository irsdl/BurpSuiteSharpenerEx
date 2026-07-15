// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.mainTabs;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import javax.swing.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

public final class MainTabsListeners implements ContainerListener {
    private final ExtensionSharedParameters sharedParameters;
    private boolean isResetInProgress = false;

    public MainTabsListeners(ExtensionSharedParameters sharedParameters) {
        this.sharedParameters = sharedParameters;
        addTabListener(sharedParameters.get_rootTabbedPaneUsingMontoya());
    }

    public void addTabListener(JTabbedPane tabbedPane) {
        sharedParameters.printDebugMessage("addMainTabListener");
        tabbedPane.addContainerListener(this);
    }

    public void removeTabListener(JTabbedPane tabbedPane) {
        sharedParameters.printDebugMessage("removeMainTabListener");
        tabbedPane.removeContainerListener(this);
    }

    @Override
    public void componentAdded(ContainerEvent e) {
        if (e.getSource() instanceof JTabbedPane && !isResetInProgress) {
            setResetInProgress(true);
            sharedParameters.delayedTasks.schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            if (sharedParameters.isUnloaded())
                                return;
                            SwingUtilities.invokeLater(() -> {
                                if (sharedParameters.isUnloaded())
                                    return;
                                MainTabsStyleHandler.resetMainTabsStylesFromSettings_noUiLock(sharedParameters);
                                setResetInProgress(false);
                            });
                        }
                    },
                    2000 // 2 seconds-delay to ensure all has been settled!
            );
        }

    }

    @Override
    public void componentRemoved(ContainerEvent e) {

    }

    public void setResetInProgress(boolean resetInProgress) {
        isResetInProgress = resetInProgress;
    }


}
