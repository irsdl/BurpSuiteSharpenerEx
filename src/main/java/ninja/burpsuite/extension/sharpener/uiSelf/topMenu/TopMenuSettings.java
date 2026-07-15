// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiSelf.topMenu;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.objects.PreferenceObject;
import ninja.burpsuite.libs.objects.StandardSettings;

import java.util.Collection;
import java.util.Collections;

public class TopMenuSettings extends StandardSettings {

    public TopMenuSettings(ExtensionSharedParameters sharedParameters) {
        super(sharedParameters);
        sharedParameters.printDebugMessage("TopMenuSettings");
    }

    @Override
    public void init() {

    }

    @Override
    public Collection<PreferenceObject> definePreferenceObjectCollection() {
        return Collections.emptyList();
    }

    @Override
    public void loadSettings() {
        // Adding the top menu
        try {

        } catch (Exception e) {
            sharedParameters.montoyaApi.logging().logToError("Error in creating the top menu: " + e.getMessage());
        }
    }

    @Override
    public void unloadSettings() {

    }
}
