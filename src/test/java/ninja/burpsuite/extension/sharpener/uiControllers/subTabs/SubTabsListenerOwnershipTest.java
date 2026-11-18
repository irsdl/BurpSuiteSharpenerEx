// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import ninja.burpsuite.extension.sharpener.ExtensionGeneralSettings;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpUIDiscovery;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.burp.generic.ExtendedPreferences;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubTabsListenerOwnershipTest {
    @Test void refreshAndUnloadPreserveHostListenersEvenAfterPaneReplacement() {
        BurpUIDiscovery.onEdt(() -> {
            ExtensionSharedParameters parameters = mock(ExtensionSharedParameters.class);
            parameters.preferences = mock(ExtendedPreferences.class);
            parameters.allSettings = mock(ExtensionGeneralSettings.class);
            parameters.allSettings.subTabsSettings = mock(SubTabsSettingsV2.class);
            parameters.subTabPreviouslySelectedIndexHistory = new HashMap<>();
            parameters.subTabNextlySelectedIndexHistory = new HashMap<>();
            doCallRealMethod().when(parameters).runOptionalFeature(anyString(), any());
            when(parameters.getAllSubTabSupportedTabs()).thenReturn(Set.of(BurpUITools.MainTabs.Repeater));
            JTabbedPane original = new JTabbedPane();
            PropertyChangeListener hostProperty = event -> {};
            ComponentListener hostComponent = new ComponentAdapter() {};
            ContainerListener hostContainer = new ContainerAdapter() {};
            KeyListener hostKey = new KeyAdapter() {};
            MouseListener hostMouse = new MouseAdapter() {};
            ChangeListener hostChange = event -> {};
            original.addPropertyChangeListener("indexForTabComponent", hostProperty);
            original.addComponentListener(hostComponent);
            original.addContainerListener(hostContainer);
            original.addKeyListener(hostKey);
            original.addMouseListener(hostMouse);
            original.addChangeListener(hostChange);
            int propertyCount = original.getPropertyChangeListeners("indexForTabComponent").length;
            int componentCount = original.getComponentListeners().length;
            int containerCount = original.getContainerListeners().length;
            int mouseCount = original.getMouseListeners().length;
            int changeCount = original.getChangeListeners().length;
            when(parameters.get_toolTabbedPane(BurpUITools.MainTabs.Repeater)).thenReturn(original);
            SubTabsListenersV2 listeners = new SubTabsListenersV2(parameters, event -> {});
            listeners.refreshListeners();
            assertEquals(propertyCount + 1, original.getPropertyChangeListeners("indexForTabComponent").length);
            original.addTab("new tab", new JPanel());
            verify(parameters.allSettings.subTabsSettings, atLeastOnce()).scheduleTabRescan(BurpUITools.MainTabs.Repeater);
            JTabbedPane replacement = new JTabbedPane();
            when(parameters.get_toolTabbedPane(BurpUITools.MainTabs.Repeater)).thenReturn(replacement);
            listeners.refreshListeners();
            assertEquals(propertyCount, original.getPropertyChangeListeners("indexForTabComponent").length);
            assertEquals(componentCount, original.getComponentListeners().length);
            assertEquals(containerCount, original.getContainerListeners().length);
            assertEquals(mouseCount, original.getMouseListeners().length);
            assertEquals(changeCount, original.getChangeListeners().length);
            assertArrayEquals(new KeyListener[]{hostKey}, original.getKeyListeners());
            listeners.removeTabListener();
            assertEquals(0, replacement.getPropertyChangeListeners("indexForTabComponent").length);
            assertEquals(0, replacement.getComponentListeners().length);
            return null;
        });
    }
}
