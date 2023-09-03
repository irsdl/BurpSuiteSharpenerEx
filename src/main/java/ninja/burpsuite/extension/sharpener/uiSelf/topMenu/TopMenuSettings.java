// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

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
