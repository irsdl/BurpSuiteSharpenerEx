// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.objects;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpExtensionSharedParameters;

import java.util.Collection;

public abstract class StandardSettings {
    private Collection<PreferenceObject> _preferenceObjectCollection;
    public ExtensionSharedParameters sharedParameters;

    // the constructor runs the settings lifecycle, so calling overridable methods here is intended
    // "all" is included because the Eclipse compiler does not know the javac "this-escape" token
    @SuppressWarnings({"all", "this-escape"})
    protected StandardSettings(ExtensionSharedParameters sharedParameters) {
        boolean isError = false;
        this.sharedParameters = sharedParameters;
        try {
            init();
            this._preferenceObjectCollection = definePreferenceObjectCollection();
            registerSettings();
            loadSettings();
        } catch (Exception e) {
            isError = true;
            sharedParameters.printException(e);
        }

        if (isError) {
            sharedParameters.printlnError("A fatal error has occurred in loading the settings. The extension is going to be unloaded.");
            sharedParameters.montoyaApi.extension().unload();
        }
    }

    public abstract void init();

    public abstract Collection<PreferenceObject> definePreferenceObjectCollection();

    public abstract void loadSettings();

    public abstract void unloadSettings();

    public Collection<PreferenceObject> get_preferenceObjectCollection() {
        return _preferenceObjectCollection;
    }

    private void registerSettings() {
        if (_preferenceObjectCollection == null)
            return;

        for (PreferenceObject preferenceObject : _preferenceObjectCollection) {
            try {
                sharedParameters.preferences.register(preferenceObject.settingName, preferenceObject.type, preferenceObject.defaultValue, preferenceObject.visibility);
            } catch (Exception e) {
                //already registered setting
                sharedParameters.printDebugMessage(e.getMessage());
                if (sharedParameters.debugLevel == BurpExtensionSharedParameters.DebugLevels.VeryVerbose.getValue())
                    sharedParameters.montoyaApi.logging().logToError(e);
            }
        }
    }

    public void resetSettings() {
        if (_preferenceObjectCollection == null)
            return;

        for (PreferenceObject preferenceObject : _preferenceObjectCollection) {
            try {
                // sharedParameters.preferences.resetSetting(preferenceObject.settingName); // there is a bug in the library, so we can't use this for now
                sharedParameters.preferences.safeSetSetting(preferenceObject.settingName, null, preferenceObject.visibility);
            } catch (Exception e) {
                sharedParameters.printDebugMessage(e.getMessage());
                if (sharedParameters.debugLevel == BurpExtensionSharedParameters.DebugLevels.VeryVerbose.getValue())
                    sharedParameters.montoyaApi.logging().logToError(e);
            }
        }
    }
}
