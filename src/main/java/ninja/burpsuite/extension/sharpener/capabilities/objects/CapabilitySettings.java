package ninja.burpsuite.extension.sharpener.capabilities.objects;

import com.coreyd97.BurpExtenderUtilities.Preferences;
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
            sharedParameters.preferences.registerSetting(capability.settingName, boolean.class, false, Preferences.Visibility.GLOBAL);
        } catch (Exception e) {
            //already registered setting
            sharedParameters.printDebugMessage(e.getMessage());
            if (sharedParameters.debugLevel == BurpExtensionSharedParameters.DebugLevels.VeryVerbose.getValue())
                sharedParameters.montoyaApi.logging().logToError(e);
        }
    }

    public boolean isEnabled() {
        return sharedParameters.preferences.safeGetSetting(capability.settingName, false);
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
