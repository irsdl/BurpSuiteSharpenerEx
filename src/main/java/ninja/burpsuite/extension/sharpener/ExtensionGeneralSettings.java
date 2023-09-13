// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;
import ninja.burpsuite.extension.sharpener.uiControllers.burpFrame.BurpFrameSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.mainTabs.MainTabsSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.subTabs.SubTabsSettingsV2;
import ninja.burpsuite.extension.sharpener.uiSelf.contextMenu.ContextMenuSettings;
import ninja.burpsuite.extension.sharpener.uiSelf.suiteTab.SuiteTabSettings;
import ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenuSettings;
import ninja.burpsuite.libs.objects.PreferenceObject;
import ninja.burpsuite.libs.objects.StandardSettings;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

public class ExtensionGeneralSettings extends StandardSettings {
    public SubTabsSettingsV2 subTabsSettings;
    public MainTabsSettings mainTabsSettings;
    public BurpFrameSettings burpFrameSettings;
    public TopMenuSettings topMenuSettings;
    public ContextMenuSettings contextMenuSettings;
    public SuiteTabSettings suiteTabSettings;

    public ArrayList<CapabilitySettings> capabilitySettingsList;

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
        if (sharedParameters.features.hasTopMenu) {
            topMenuSettings = new TopMenuSettings(sharedParameters);
        }
        if (sharedParameters.features.hasContextMenu) {
            contextMenuSettings = new ContextMenuSettings(sharedParameters);
        }
        if (sharedParameters.features.hasSuiteTab) {
            suiteTabSettings = new SuiteTabSettings(sharedParameters);
        }

        if (sharedParameters.preferences.safeGetSetting("checkForUpdate", false)) {
            ExtensionMainClass sharpenerBurpExtension = (ExtensionMainClass) sharedParameters.burpExtender;
            sharpenerBurpExtension.checkForUpdate();
        }

        burpFrameSettings = new BurpFrameSettings(sharedParameters);
        mainTabsSettings = new MainTabsSettings(sharedParameters);
        subTabsSettings = new SubTabsSettingsV2(sharedParameters);

        capabilityInitializer();

    }

    private void capabilityInitializer() {
        // Load the package and get all classes
        capabilitySettingsList = new ArrayList<>();
        try {
            // Use Reflections to get all subclasses of CapabilitySettings in the specified package
            Reflections reflections = new Reflections("ninja.burpsuite.extension.sharpener.capabilities.implementations");
            Set<Class<? extends CapabilitySettings>> subTypes = reflections.getSubTypesOf(CapabilitySettings.class);

            for (Class<? extends CapabilitySettings> clazz : subTypes) {
                // Get the constructor that accepts ExtensionSharedParameters as a parameter
                Constructor<? extends CapabilitySettings> constructor = clazz.getConstructor(ExtensionSharedParameters.class);

                // Initialize the class using the constructor and cast it to CapabilitySettings
                CapabilitySettings capabilitySettingsInstance = constructor.newInstance(sharedParameters);

                capabilitySettingsList.add(capabilitySettingsInstance);
            }
        } catch (Exception e) {
            sharedParameters.printException(e, "Capabilities could not be loaded!");
        }

        capabilitySettingsList.sort(Comparator.comparingInt(CapabilitySettings::getOrder));
    }

    @Override
    public void unloadSettings() {
        if (sharedParameters.features.hasTopMenu && topMenuSettings != null) {
            topMenuSettings.unloadSettings();
        }

        if (burpFrameSettings != null) {
            burpFrameSettings.unloadSettings();
        }

        if (mainTabsSettings != null) {
            mainTabsSettings.unloadSettings();
        }

        if (subTabsSettings != null) {
            subTabsSettings.unloadSettings();
        }

        // unload capability settings
        for (CapabilitySettings capabilitySettings : capabilitySettingsList) {
            capabilitySettings.unloadSettings();
        }
    }
}
