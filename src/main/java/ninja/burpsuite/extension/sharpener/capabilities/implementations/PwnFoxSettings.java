// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.objects.Capability;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilityGroup;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;
import ninja.burpsuite.libs.objects.PreferenceObject;

import java.util.Arrays;
import java.util.Collection;

public class PwnFoxSettings extends CapabilitySettings {
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
        return null;
    }

    @Override
    public void loadSettings() {

    }

    @Override
    public void unloadSettings() {

    }
}
