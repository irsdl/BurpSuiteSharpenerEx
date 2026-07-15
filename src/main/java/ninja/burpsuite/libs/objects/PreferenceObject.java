// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.objects;

import com.coreyd97.BurpExtenderUtilities.Preferences;

import java.lang.reflect.Type;

public class PreferenceObject {
    public String settingName;
    public Type type;
    public Object defaultValue;
    public Preferences.Visibility visibility;

    public PreferenceObject(String settingName, Type type, Object defaultValue, Preferences.Visibility visibility) {
        this.settingName = settingName;
        this.type = type;
        this.defaultValue = defaultValue;
        this.visibility = visibility;
    }
}
