// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import ninja.burpsuite.extension.sharpener.capabilities.pwnFox.PwnFoxSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.burpFrame.BurpFrameSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.mainTabs.MainTabsSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.subTabs.SubTabsSettingsV2;
import ninja.burpsuite.extension.sharpener.uiSelf.contextMenu.ContextMenuSettings;
import ninja.burpsuite.extension.sharpener.uiSelf.suiteTab.SuiteTabSettings;
import ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenuSettings;
import ninja.burpsuite.libs.objects.PreferenceObject;
import ninja.burpsuite.libs.objects.StandardSettings;

import java.util.ArrayList;
import java.util.Collection;

public class ExtensionGeneralSettings extends StandardSettings {
    public SubTabsSettingsV2 subTabsSettings;
    public MainTabsSettings mainTabsSettings;
    public BurpFrameSettings burpFrameSettings;
    public TopMenuSettings topMenuSettings;
    public ContextMenuSettings contextMenuSettings;
    public SuiteTabSettings suiteTabSettings;
    public PwnFoxSettings pwnFoxSettings;

    public ExtensionGeneralSettings(ExtensionSharedParameters sharedParameters) {
        super(sharedParameters);
    }

    @Override
    public void init() {

    }

    @Override
    public Collection<PreferenceObject> definePreferenceObjectCollection() {
        Collection<PreferenceObject> preferenceObjectCollection = new ArrayList<>();
        PreferenceObject preferenceObject;
        try {
            preferenceObject = new PreferenceObject("checkForUpdate", boolean.class, false, Preferences.Visibility.GLOBAL);
            preferenceObjectCollection.add(preferenceObject);
        } catch (Exception e) {
            //already registered setting
            sharedParameters.printDebugMessage(e.getMessage());
        }

        return preferenceObjectCollection;


    }

    @Override
    public void loadSettings() {
        if(sharedParameters.features.hasTopMenu){
            topMenuSettings = new TopMenuSettings(sharedParameters);
        }
        if(sharedParameters.features.hasContextMenu){
            contextMenuSettings= new ContextMenuSettings(sharedParameters);
        }
        if(sharedParameters.features.hasSuiteTab){
            suiteTabSettings= new SuiteTabSettings(sharedParameters);
        }

        if (sharedParameters.preferences.safeGetSetting("checkForUpdate", false)) {
            ExtensionMainClass sharpenerBurpExtension = (ExtensionMainClass) sharedParameters.burpExtender;
            sharpenerBurpExtension.checkForUpdate();
        }

        burpFrameSettings = new BurpFrameSettings(sharedParameters);
        mainTabsSettings = new MainTabsSettings(sharedParameters);
        subTabsSettings = new SubTabsSettingsV2(sharedParameters);
        pwnFoxSettings = new PwnFoxSettings(sharedParameters);
    }

    @Override
    public void unloadSettings() {
        if(sharedParameters.features.hasTopMenu && topMenuSettings != null){
            topMenuSettings.unloadSettings();
        }

        if(burpFrameSettings!=null){
            burpFrameSettings.unloadSettings();
        }

        if(mainTabsSettings != null){
            mainTabsSettings.unloadSettings();
        }

        if(subTabsSettings != null){
            subTabsSettings.unloadSettings();
        }

        if(pwnFoxSettings != null){
            pwnFoxSettings.unloadSettings();
        }
    }
}
