// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.capabilities.objects;

import ninja.burpsuite.libs.thirdparty.burpextenderutilities.Preferences;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpExtensionSharedParameters;
import ninja.burpsuite.libs.objects.StandardSettings;

public abstract class CapabilitySettings extends StandardSettings {
    public Capability capability;

    public CapabilitySettings(ExtensionSharedParameters sharedParameters, Capability capability) {
        super(sharedParameters);
        this.capability = capability;
        registerStateSetting();
    }

    private void registerStateSetting() {
        try {
            sharedParameters.preferences.register(capability.settingName, boolean.class, capability.enabledByDefault, Preferences.Visibility.GLOBAL);
        } catch (Exception e) {
            //already registered setting
            sharedParameters.printDebugMessage(e.getMessage());
            if (sharedParameters.debugLevel == BurpExtensionSharedParameters.DebugLevels.VeryVerbose.getValue())
                sharedParameters.montoyaApi.logging().logToError(e);
        }
    }

    public boolean isEnabled() {return sharedParameters.preferences.safeGetSetting(capability.settingName, capability.enabledByDefault);
    }

    public void setEnabled(boolean enabled) {
        sharedParameters.preferences.safeSetSetting(capability.settingName, enabled, Preferences.Visibility.GLOBAL);
    }

    public static <T> int getOrder(T t) {
        if (t instanceof CapabilitySettings) {
            return ((CapabilitySettings) t).capability.order;
        }
        return -1;
    }
}
