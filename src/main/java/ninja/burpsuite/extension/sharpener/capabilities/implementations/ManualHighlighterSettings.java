// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.objects.Capability;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilityGroup;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;
import ninja.burpsuite.libs.objects.PreferenceObject;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.Preferences;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ManualHighlighterSettings extends CapabilitySettings {
    public static final String LIMIT_LOGGER_INSPECTION_SETTING_NAME = "manualHighlightLimitLoggerInspection";
    public static final int LOGGER_INSPECTION_BYTE_LIMIT = 65_536;
    public ManualHighlighterSettings(ExtensionSharedParameters sharedParameters) {
        super(sharedParameters,
                new Capability("Manual Highlighter (Proxy and Logger)",
                        "Highlight Proxy and Logger traffic using tempcolorCOLORNAME or permcolorCOLORNAME. Logger inspects the first 65,536 raw message bytes by default; disable its limit for complete inspection. Proxy temporary-marker removal is unchanged; permcolor is retained.",
                        "manualHighlightCapability",
                        Arrays.asList(CapabilityGroup.PROXY_REQUEST_HANDLER, CapabilityGroup.PROXY_RESPONSE_HANDLER, CapabilityGroup.WEBSOCKET_CREATION_HANDLER, CapabilityGroup.HTTP_HANDLER),
                        "ninja.burpsuite.extension.sharpener.capabilities.implementations.ManualHighlighterRequestResponseHandler",
                        10000, true));
    }

    @Override
    public void init() {

    }

    @Override
    public Collection<PreferenceObject> definePreferenceObjectCollection() {
        return List.of(new PreferenceObject(LIMIT_LOGGER_INSPECTION_SETTING_NAME,
                boolean.class, true, Preferences.Visibility.GLOBAL));
    }

    public boolean isLoggerInspectionLimited() {
        return sharedParameters.preferences.safeGetSetting(LIMIT_LOGGER_INSPECTION_SETTING_NAME, true);
    }

    public void setLoggerInspectionLimited(boolean limited) {
        sharedParameters.preferences.safeSetSetting(LIMIT_LOGGER_INSPECTION_SETTING_NAME, limited, Preferences.Visibility.GLOBAL);
    }

    @Override
    public void loadSettings() {

    }

    @Override
    public void unloadSettings() {

    }
}
