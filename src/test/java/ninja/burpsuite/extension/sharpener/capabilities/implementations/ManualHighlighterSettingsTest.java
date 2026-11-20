// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.ExtendedPreferences;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.*;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.nameManager.NameManager;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManualHighlighterSettingsTest {
    static final String KEY=ManualHighlighterSettings.LIMIT_LOGGER_INSPECTION_SETTING_NAME;
    final InMemoryPersistence store=new InMemoryPersistence();
    ExtensionSharedParameters parameters;
    @BeforeEach void setup(){release();parameters=mock(ExtensionSharedParameters.class);}
    @AfterEach void cleanup(){release();}
    void release(){for(String key:new String[]{KEY,"manualHighlightCapability"})if(NameManager.isReserved(key))NameManager.release(key);}
    ManualHighlighterSettings load(){
        parameters.preferences=new ExtendedPreferences(store.montoyaApi(),new DefaultGsonProvider());
        return new ManualHighlighterSettings(parameters);
    }
    @Test void existingInstallationDefaultsOnAndPersistedOffSurvivesReloadAndCapabilityToggle(){
        var setting=load();assertTrue(setting.isLoaded());assertTrue(setting.isLoggerInspectionLimited());
        assertEquals(Preferences.Visibility.GLOBAL,parameters.preferences.getRegisteredSettings().get(KEY));
        setting.setLoggerInspectionLimited(false);assertEquals("false",store.globalStore.get(KEY));assertFalse(store.projectStore.containsKey(KEY));
        setting.setEnabled(false);assertFalse(setting.isLoggerInspectionLimited());
        release();setting=load();assertFalse(setting.isEnabled());assertFalse(setting.isLoggerInspectionLimited());
        setting.setEnabled(true);assertFalse(setting.isLoggerInspectionLimited());
        setting.setLoggerInspectionLimited(true);assertTrue(setting.isLoggerInspectionLimited());
        setting.setLoggerInspectionLimited(false);parameters.preferences.resetAll();assertTrue(setting.isLoggerInspectionLimited());
        release();assertTrue(load().isLoggerInspectionLimited());
    }
}
