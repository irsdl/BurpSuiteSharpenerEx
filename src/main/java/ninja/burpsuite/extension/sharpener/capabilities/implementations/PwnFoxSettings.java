// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.objects.Capability;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilityGroup;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;
import ninja.burpsuite.libs.objects.PreferenceObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PwnFoxSettings extends CapabilitySettings {
    // when true, the X-PwnFox-Color header is removed before the request is sent
    public static final String REMOVE_COLOR_HEADER_SETTING_NAME = "pwnFoxRemoveColorHeader";

    public PwnFoxSettings(ExtensionSharedParameters sharedParameters) {
        super(sharedParameters,
                new Capability("PwnFox Highlighter",
                        "Useful when PwnFox extension is enabled in Firefox",
                        "pwnFoxSupportCapability",
                        Arrays.asList(CapabilityGroup.PROXY_REQUEST_HANDLER),
                        "ninja.burpsuite.extension.sharpener.capabilities.implementations.PwnFoxProxyRequestHandler",
                        50000, true));
    }

    @Override
    public void init() {

    }

    @Override
    public Collection<PreferenceObject> definePreferenceObjectCollection() {
        return List.of(new PreferenceObject(REMOVE_COLOR_HEADER_SETTING_NAME,
                boolean.class, true, Preferences.Visibility.GLOBAL));
    }

    @Override
    public void loadSettings() {

    }

    @Override
    public void unloadSettings() {

    }

    public boolean isHeaderRemovalEnabled() {
        return sharedParameters.preferences.safeGetSetting(REMOVE_COLOR_HEADER_SETTING_NAME, true);
    }

    public void setHeaderRemovalEnabled(boolean enabled) {
        sharedParameters.preferences.safeSetSetting(REMOVE_COLOR_HEADER_SETTING_NAME, enabled, Preferences.Visibility.GLOBAL);
    }
}
