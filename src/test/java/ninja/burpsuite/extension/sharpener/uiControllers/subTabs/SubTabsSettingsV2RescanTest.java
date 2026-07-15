// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import ninja.burpsuite.extension.sharpener.ExtensionGeneralSettings;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.burp.generic.ExtendedPreferences;
import ninja.burpsuite.libs.generic.DelayedTaskRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// These tests cover the recovery path for tabs that were missed by the tab change
// listener. A missed tab used to stay unknown to Sharpener until the user did a drag
// and drop or added another tab. Now interacting with the missed tab schedules a
// delayed rescan which detects it.
public class SubTabsSettingsV2RescanTest {

    private ExtensionSharedParameters sharedParameters;
    private SubTabsSettingsV2 subTabsSettings;
    private JTabbedPane toolTabbedPane;

    @BeforeEach
    void setUp() throws Exception {
        sharedParameters = mock(ExtensionSharedParameters.class);
        sharedParameters.preferences = mock(ExtendedPreferences.class);
        sharedParameters.allSubTabContainerHandlers = new HashMap<>();
        sharedParameters.allSubTabContainerHandlers.put(BurpUITools.MainTabs.Repeater, new ArrayList<>());
        sharedParameters.supportedTools_SubTabs = new HashMap<>();
        sharedParameters.supportedTools_SubTabs.put(BurpUITools.MainTabs.Repeater, new HashMap<>());
        sharedParameters.isTabGroupSupportedByDefault = true;
        sharedParameters.isTabTextColorSetByBackground = false;
        sharedParameters.isDarkMode = false;
        injectDelayedTasks(sharedParameters, new DelayedTaskRunner());

        // no tool is accessible yet, so the constructor does not touch any Burp UI
        subTabsSettings = new SubTabsSettingsV2(sharedParameters);

        JTabbedPane mainTabbedPane = new JTabbedPane();
        toolTabbedPane = new JTabbedPane();
        mainTabbedPane.addTab("Repeater", toolTabbedPane);
        addSubTab(toolTabbedPane, "1");
        addSubTab(toolTabbedPane, "2");

        when(sharedParameters.getAccessibleSubTabSupportedTabs()).thenReturn(Set.of(BurpUITools.MainTabs.Repeater));
        when(sharedParameters.get_toolTabbedPane(BurpUITools.MainTabs.Repeater)).thenReturn(toolTabbedPane);
    }

    // the delayedTasks field is final, so the test injects a real runner into the mock by reflection
    private static void injectDelayedTasks(ExtensionSharedParameters mockedParameters, DelayedTaskRunner runner) throws Exception {
        Field delayedTasksField = BurpExtensionSharedParameters.class.getField("delayedTasks");
        delayedTasksField.setAccessible(true);
        delayedTasksField.set(mockedParameters, runner);
    }

    private JTextField addSubTab(JTabbedPane tabbedPane, String title) {
        tabbedPane.addTab(title, new JPanel());
        JPanel tabComponent = new JPanel();
        JTextField titleTextField = new JTextField(title);
        tabComponent.add(titleTextField);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabComponent);
        return titleTextField;
    }

    private SubTabsContainerHandler createHandler(int tabIndex) {
        return new SubTabsContainerHandler(sharedParameters, toolTabbedPane, tabIndex, false);
    }

    private ArrayList<SubTabsContainerHandler> repeaterHandlers() {
        return sharedParameters.allSubTabContainerHandlers.get(BurpUITools.MainTabs.Repeater);
    }

    private boolean tabHasHandler(int tabIndex) {
        Component tabComponent = toolTabbedPane.getTabComponentAt(tabIndex);
        for (SubTabsContainerHandler handler : repeaterHandlers()) {
            if (tabComponent.equals(handler.currentTabContainer)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void rescanDelayIsShortOnNewBurp() {
        sharedParameters.isTabTextColorSetByBackground = false;
        assertEquals(1000, subTabsSettings.getTabRescanDelayMs(BurpUITools.MainTabs.Repeater));
        assertEquals(1000, subTabsSettings.getTabRescanDelayMs(BurpUITools.MainTabs.Intruder));
    }

    @Test
    void rescanDelayIsLongerOnOldBurpToKeepTheColourSafe() {
        sharedParameters.isTabTextColorSetByBackground = true;
        assertEquals(3000, subTabsSettings.getTabRescanDelayMs(BurpUITools.MainTabs.Repeater));
        assertEquals(10000, subTabsSettings.getTabRescanDelayMs(BurpUITools.MainTabs.Intruder));
    }

    @Test
    void onlyOneRescanIsPendingPerTool() {
        assertTrue(subTabsSettings.scheduleTabRescan(BurpUITools.MainTabs.Repeater));
        assertFalse(subTabsSettings.scheduleTabRescan(BurpUITools.MainTabs.Repeater));
        // another tool is not blocked by the pending Repeater rescan
        assertTrue(subTabsSettings.scheduleTabRescan(BurpUITools.MainTabs.Intruder));
    }

    @Test
    void rescanIsRejectedForInvalidTools() {
        assertFalse(subTabsSettings.scheduleTabRescan(null));
        assertFalse(subTabsSettings.scheduleTabRescan(BurpUITools.MainTabs.None));
    }

    @Test
    void rescanDetectsATabThatWasMissed() throws Exception {
        // only the first tab is known, the second tab was missed by the listener
        repeaterHandlers().add(createHandler(0));
        assertFalse(tabHasHandler(1));

        assertTrue(subTabsSettings.scheduleTabRescan(BurpUITools.MainTabs.Repeater));

        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline && !tabHasHandler(1)) {
            Thread.sleep(100);
        }

        assertTrue(tabHasHandler(1), "the missed tab must get a handler after the rescan");
        // once the rescan has run, a new rescan can be scheduled again
        assertTrue(subTabsSettings.scheduleTabRescan(BurpUITools.MainTabs.Repeater));
    }

    @Test
    void updateHandlersDetectsAMissedTabWhenTheTabCountHasNotChanged() {
        // tab "3" is created, gets a handler, and is then closed. The stale handler keeps
        // the handler count equal to the tab count while tab "2" has no handler at all.
        // This mimics a close and add sequence that happened while an update was pending.
        repeaterHandlers().add(createHandler(0));
        addSubTab(toolTabbedPane, "3");
        SubTabsContainerHandler staleHandler = createHandler(2);
        repeaterHandlers().add(staleHandler);
        toolTabbedPane.removeTabAt(2);

        assertFalse(staleHandler.isValid());
        assertEquals(toolTabbedPane.getTabCount(), repeaterHandlers().size());
        assertFalse(tabHasHandler(1));

        subTabsSettings.updateAllSubTabContainerHandlersObj(BurpUITools.MainTabs.Repeater);

        assertTrue(tabHasHandler(1), "the missed tab must get a handler even when the counts match");
        assertFalse(repeaterHandlers().contains(staleHandler), "the stale handler must be dropped");
        for (SubTabsContainerHandler handler : repeaterHandlers()) {
            assertTrue(handler.isValid());
        }
    }

    @Test
    void interactionWithAnUnknownTabSchedulesARescan() {
        SubTabsSettingsV2 mockedSubTabsSettings = mock(SubTabsSettingsV2.class);
        sharedParameters.allSettings = mock(ExtensionGeneralSettings.class);
        sharedParameters.allSettings.subTabsSettings = mockedSubTabsSettings;

        SubTabsContainerHandler result = SubTabsActions.getSubTabContainerHandlerFromSharedParameters(sharedParameters, toolTabbedPane, 1);

        assertNull(result);
        verify(mockedSubTabsSettings).scheduleTabRescan(BurpUITools.MainTabs.Repeater);
    }

    @Test
    void interactionWithAKnownTabDoesNotScheduleARescan() {
        repeaterHandlers().add(createHandler(0));
        SubTabsSettingsV2 mockedSubTabsSettings = mock(SubTabsSettingsV2.class);
        sharedParameters.allSettings = mock(ExtensionGeneralSettings.class);
        sharedParameters.allSettings.subTabsSettings = mockedSubTabsSettings;

        SubTabsContainerHandler result = SubTabsActions.getSubTabContainerHandlerFromSharedParameters(sharedParameters, toolTabbedPane, 0);

        assertNotNull(result);
        verify(mockedSubTabsSettings, never()).scheduleTabRescan(any());
    }
}
