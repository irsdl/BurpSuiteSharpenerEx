// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiSelf.contextMenu;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.objects.PreferenceObject;
import ninja.burpsuite.libs.objects.StandardSettings;

import java.util.Collection;

public class ContextMenuSettings extends StandardSettings {
    public ContextMenuSettings(ExtensionSharedParameters sharedParameters) {
        super(sharedParameters);

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
