// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener;

import ninja.burpsuite.libs.thirdparty.burpextenderutilities.Preferences;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;
import ninja.burpsuite.extension.sharpener.uiControllers.burpFrame.BurpFrameSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.mainTabs.MainTabsSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.subTabs.SubTabsSettingsV2;
import ninja.burpsuite.extension.sharpener.uiSelf.contextMenu.ContextMenuSettings;
import ninja.burpsuite.extension.sharpener.uiSelf.suiteTab.SuiteTabSettings;
import ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenuSettings;
import ninja.burpsuite.libs.objects.PreferenceObject;
import ninja.burpsuite.libs.objects.StandardSettings;
import ninja.burpsuite.libs.burp.generic.BurpUITools;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
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
    private Timer uiMonitor;
    private List<JTabbedPane> lastUiBindings;

    // Runs on the EDT. Compare identities rather than Burp release/channel strings.
    public void startUiMonitoring() {
        if (uiMonitor != null) return;
        lastUiBindings = currentUiBindings();
        uiMonitor = new Timer(2000, event -> sharedParameters.runOptionalFeature(
                "UI layout recovery", this::refreshUiBindingsIfChanged));
        uiMonitor.start();
    }

    private List<JTabbedPane> currentUiBindings() {
        return Arrays.asList(sharedParameters.get_rootTabbedPaneUsingMontoya(),
                sharedParameters.get_toolTabbedPane(BurpUITools.MainTabs.Repeater),
                sharedParameters.get_toolTabbedPane(BurpUITools.MainTabs.Intruder));
    }

    void refreshUiBindingsIfChanged() {
        if (sharedParameters.isUnloaded()) {
            if (uiMonitor != null) uiMonitor.stop();
            return;
        }
        List<JTabbedPane> bindings = currentUiBindings();
        if (bindings.equals(lastUiBindings)) return;
        boolean mainChanged = lastUiBindings == null || bindings.getFirst() != lastUiBindings.getFirst();
        lastUiBindings = bindings;
        if (mainChanged && mainTabsSettings != null)
            sharedParameters.runOptionalFeature("main tab reattachment", mainTabsSettings::loadSettings);
        if (subTabsSettings != null) {
            sharedParameters.runOptionalFeature("sub-tab reattachment", () -> {
                subTabsSettings.isFirstLoad = true;
                subTabsSettings.loadSettings();
            });
        }
    }

    public ExtensionGeneralSettings(ExtensionSharedParameters sharedParameters) {
        super(sharedParameters);
    }

    @Override
    public void init() {
        capabilitySettingsList = new ArrayList<>();
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
            sharedParameters.runOptionalFeature("TopMenuSettings", () -> topMenuSettings = new TopMenuSettings(sharedParameters));
        }
        if (sharedParameters.features.hasContextMenu) {
            sharedParameters.runOptionalFeature("ContextMenuSettings", () -> contextMenuSettings = new ContextMenuSettings(sharedParameters));
        }
        if (sharedParameters.features.hasSuiteTab) {
            sharedParameters.runOptionalFeature("SuiteTabSettings", () -> suiteTabSettings = new SuiteTabSettings(sharedParameters));
        }

        if (sharedParameters.preferences.safeGetSetting("checkForUpdate", false)) {
            ExtensionMainClass sharpenerBurpExtension = (ExtensionMainClass) sharedParameters.burpExtender;
            sharpenerBurpExtension.checkForUpdate();
        }

        sharedParameters.runOptionalFeature("BurpFrameSettings", () -> burpFrameSettings = new BurpFrameSettings(sharedParameters));
        sharedParameters.runOptionalFeature("MainTabsSettings", () -> mainTabsSettings = new MainTabsSettings(sharedParameters));
        sharedParameters.runOptionalFeature("SubTabsSettingsV2", () -> subTabsSettings = new SubTabsSettingsV2(sharedParameters));

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
                try {
                    Constructor<? extends CapabilitySettings> constructor = clazz.getConstructor(ExtensionSharedParameters.class);
                    CapabilitySettings settings = constructor.newInstance(sharedParameters);
                    if (settings.isLoaded()) capabilitySettingsList.add(settings);
                } catch (ReflectiveOperationException | LinkageError failure) {
                    sharedParameters.reportCompatibilityIssue(clazz.getSimpleName(), failure);
                }
            }
        } catch (Exception e) {
            sharedParameters.printException(e, "Capabilities could not be loaded!");
        }

        capabilitySettingsList.sort(Comparator.comparingInt(CapabilitySettings::getOrder));
    }

    @Override
    public void unloadSettings() {
        if (uiMonitor != null) uiMonitor.stop();
        if (sharedParameters.features.hasTopMenu && topMenuSettings != null) {
            sharedParameters.runOptionalFeature("topMenuSettings cleanup", topMenuSettings::unloadSettings);
        }

        if (burpFrameSettings != null) {
            sharedParameters.runOptionalFeature("burpFrameSettings cleanup", burpFrameSettings::unloadSettings);
        }

        if (mainTabsSettings != null) {
            sharedParameters.runOptionalFeature("mainTabsSettings cleanup", mainTabsSettings::unloadSettings);
        }

        if (subTabsSettings != null) {
            sharedParameters.runOptionalFeature("subTabsSettings cleanup", subTabsSettings::unloadSettings);
        }

        // unload capability settings
        if (capabilitySettingsList != null) {
            for (CapabilitySettings capabilitySettings : capabilitySettingsList)
                sharedParameters.runOptionalFeature(capabilitySettings.getClass().getSimpleName() + " cleanup", capabilitySettings::unloadSettings);
        }
    }
}
