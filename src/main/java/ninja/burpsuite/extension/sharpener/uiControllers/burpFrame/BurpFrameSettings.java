// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.burpFrame;

import ninja.burpsuite.libs.thirdparty.burpextenderutilities.Preferences;
import com.google.gson.reflect.TypeToken;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.uiControllers.shortcuts.ShortcutMappings;
import ninja.burpsuite.libs.burp.generic.BurpTitleAndIcon;
import ninja.burpsuite.libs.generic.UIHelper;
import ninja.burpsuite.libs.objects.PreferenceObject;
import ninja.burpsuite.libs.objects.StandardSettings;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class BurpFrameSettings extends StandardSettings {

    private BurpFrameListeners burpFrameListeners;

    public BurpFrameSettings(ExtensionSharedParameters sharedParameters) {
        super(sharedParameters);
        sharedParameters.printDebugMessage("BurpFrameSettings");
    }

    @Override
    public void init() {

    }

    @Override
    public Collection<PreferenceObject> definePreferenceObjectCollection() {
        Collection<PreferenceObject> preferenceObjectCollection = new ArrayList<>();

        String[] projectStringSettingNames = {"BurpTitle", "BurpIconCustomPath", "BurpResourceIconName"};
        String[] globalStringSettingNames = {"LastBurpIconCustomPath"};

        for (String settingName : projectStringSettingNames) {
            try {
                PreferenceObject preferenceObject = new PreferenceObject(settingName, String.class, "", Preferences.Visibility.PROJECT);
                preferenceObjectCollection.add(preferenceObject);
            } catch (Exception e) {
                //already registered setting
                sharedParameters.printDebugMessage(e.getMessage());
            }
        }

        for (String settingName : globalStringSettingNames) {
            try {
                PreferenceObject preferenceObject = new PreferenceObject(settingName, String.class, "", Preferences.Visibility.GLOBAL);
                preferenceObjectCollection.add(preferenceObject);
            } catch (Exception e) {
                //already registered setting
                sharedParameters.printDebugMessage(e.getMessage());
            }
        }

        PreferenceObject preferenceObject = new PreferenceObject("useLastScreenPositionAndSize", boolean.class, false, Preferences.Visibility.GLOBAL);
        preferenceObjectCollection.add(preferenceObject);

        preferenceObject = new PreferenceObject("detectOffScreenPosition", boolean.class, true, Preferences.Visibility.GLOBAL);
        preferenceObjectCollection.add(preferenceObject);

        preferenceObject = new PreferenceObject("lastApplicationPosition", Point.class, null, Preferences.Visibility.GLOBAL);
        preferenceObjectCollection.add(preferenceObject);

        preferenceObject = new PreferenceObject("lastApplicationSize", Dimension.class, null, Preferences.Visibility.GLOBAL);
        preferenceObjectCollection.add(preferenceObject);

        // user defined keyboard shortcuts. BurpFrameSettings loads before SubTabsSettingsV2,
        // so the setting is registered here as well; the duplicate registration is tolerated.
        preferenceObject = new PreferenceObject(ShortcutMappings.CUSTOM_SHORTCUTS_SETTING_NAME,
                new TypeToken<HashMap<String, ArrayList<String>>>() {
                }.getType(), null, Preferences.Visibility.GLOBAL);
        preferenceObjectCollection.add(preferenceObject);

        return preferenceObjectCollection;
    }

    // Reinstalls the key bindings so a shortcut change is applied without a full reload.
    public void reloadShortcuts() {
        if (burpFrameListeners != null) {
            burpFrameListeners.reloadShortcuts(sharedParameters.get_mainFrameUsingMontoya());
        }
    }

    @Override
    public void loadSettings() {
        sharedParameters.printDebugMessage("loadSettings");

        String newTitle = sharedParameters.preferences.safeGetStringSetting("BurpTitle");
        if (!newTitle.isBlank()) {
            BurpTitleAndIcon.setTitle_noUiLock(sharedParameters, newTitle);
        }

        String newIconPath = sharedParameters.preferences.safeGetStringSetting("BurpIconCustomPath");
        String newIconResourcePath = sharedParameters.preferences.safeGetStringSetting("BurpResourceIconName");
        if (!newIconPath.isBlank()) {
            sharedParameters.preferences.set("LastBurpIconCustomPath", newIconPath);
            BurpTitleAndIcon.setIcon(sharedParameters, newIconPath, false);
        } else if (!newIconResourcePath.isBlank()) {
            BurpTitleAndIcon.setIcon(sharedParameters, newIconResourcePath, true);
        }

        boolean useLastScreenPositionAndSize = sharedParameters.preferences.safeGetBooleanSetting("useLastScreenPositionAndSize");
        //boolean detectOffScreenPosition = sharedParameters.preferences.safeGetBooleanSetting("detectOffScreenPosition");

        if (useLastScreenPositionAndSize) {
            Point lastApplicationPosition = sharedParameters.preferences.safeGetSetting("lastApplicationPosition", null);
            Dimension lastApplicationSize = sharedParameters.preferences.safeGetSetting("lastApplicationSize", null);

            // a minimised window position saved by an older version (-32000 on Windows)
            // would restore Burp far off screen
            if (lastApplicationPosition != null
                    && lastApplicationPosition.x > -30000 && lastApplicationPosition.y > -30000) {
                sharedParameters.get_mainFrameUsingMontoya().setLocation(lastApplicationPosition);
            }

            // a size below the minimum would restore a practically invisible window
            if (lastApplicationSize != null
                    && !UIHelper.isSizeTooSmall(lastApplicationSize, BurpFrameListeners.MIN_VISIBLE_FRAME_SIZE)) {
                sharedParameters.get_mainFrameUsingMontoya().setSize(lastApplicationSize);
            }
        }

        burpFrameListeners = new BurpFrameListeners(sharedParameters);
    }

    @Override
    public void unloadSettings() {
        sharedParameters.printDebugMessage("reset Burp title and icon");

        if (burpFrameListeners != null) {
            burpFrameListeners.removeBurpFrameListener(sharedParameters.get_mainFrameUsingMontoya());
        }

        // reset Burp title and icon
        BurpTitleAndIcon.resetTitle(sharedParameters);
        BurpTitleAndIcon.resetIcon(sharedParameters);

    }
}
