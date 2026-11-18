// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.extension.sharpener;

import ninja.burpsuite.extension.sharpener.uiControllers.mainTabs.MainTabsSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.subTabs.SubTabsSettingsV2;
import ninja.burpsuite.libs.burp.generic.BurpExtensionFeatures;
import ninja.burpsuite.libs.burp.generic.BurpUIDiscovery;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UiLayoutRecoveryTest {
    @Test void lateOrReplacedPanesReattachOnceAndStopOnUnload() {
        BurpUIDiscovery.onEdt(() -> {
            ExtensionSharedParameters parameters = mock(ExtensionSharedParameters.class);
            parameters.features = new BurpExtensionFeatures();
            doCallRealMethod().when(parameters).runOptionalFeature(anyString(), any());
            ExtensionGeneralSettings settings = mock(ExtensionGeneralSettings.class, CALLS_REAL_METHODS);
            settings.sharedParameters = parameters;
            settings.mainTabsSettings = mock(MainTabsSettings.class);
            settings.subTabsSettings = mock(SubTabsSettingsV2.class);
            settings.startUiMonitoring();
            try {
                settings.refreshUiBindingsIfChanged();
                verify(settings.mainTabsSettings, never()).loadSettings();
                JTabbedPane main = new JTabbedPane();
                JTabbedPane repeater = new JTabbedPane();
                when(parameters.get_rootTabbedPaneUsingMontoya()).thenReturn(main);
                when(parameters.get_toolTabbedPane(BurpUITools.MainTabs.Repeater)).thenReturn(repeater);
                settings.refreshUiBindingsIfChanged();
                settings.refreshUiBindingsIfChanged();
                verify(settings.mainTabsSettings, times(1)).loadSettings();
                verify(settings.subTabsSettings, times(1)).loadSettings();
                assertTrue(settings.subTabsSettings.isFirstLoad);
                when(parameters.get_toolTabbedPane(BurpUITools.MainTabs.Repeater)).thenReturn(new JTabbedPane());
                settings.refreshUiBindingsIfChanged();
                verify(settings.mainTabsSettings, times(1)).loadSettings();
                verify(settings.subTabsSettings, times(2)).loadSettings();
                when(parameters.isUnloaded()).thenReturn(true);
                when(parameters.get_rootTabbedPaneUsingMontoya()).thenReturn(new JTabbedPane());
                settings.refreshUiBindingsIfChanged();
                verify(settings.mainTabsSettings, times(1)).loadSettings();
            } finally {
                settings.unloadSettings();
            }
            return null;
        });
    }
}
