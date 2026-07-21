// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.HashMap;

import static ninja.burpsuite.libs.thirdparty.burpextenderutilities.InMemoryPersistence.uniqueName;
import static org.junit.jupiter.api.Assertions.*;

// Characterizes reset behavior in library commit b7faf563. The Sharpener code still avoids
// reset()/resetAll() because of a historical library bug (see StandardSettings.resetSettings
// and BurpExtensionSharedParameters.resetAllSettings); these tests document what the current
// code actually does, so the workarounds can be reviewed in a follow-up change.
public class PreferencesResetTest {

    private InMemoryPersistence persistence;
    private Preferences preferences;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryPersistence();
        preferences = new Preferences(persistence.montoyaApi(), new DefaultGsonProvider());
    }

    @Test
    void resetRestoresTheRegisteredDefaultAndPersistsIt() {
        String name = uniqueName("resetSingle");
        preferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL);
        preferences.set(name, "changed");
        assertEquals("\"changed\"", persistence.globalStore.get(name));

        preferences.reset(name);

        assertEquals("default", preferences.get(name));
        assertEquals("\"default\"", persistence.globalStore.get(name));
    }

    @Test
    @SuppressWarnings("deprecation") // the deprecated alias is still referenced by the workaround comments
    void deprecatedResetSettingAliasBehavesLikeReset() {
        String name = uniqueName("resetAlias");
        preferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL);
        preferences.set(name, "changed");

        preferences.resetSetting(name);

        assertEquals("default", preferences.get(name));
    }

    @Test
    void resetAllResetsEveryRegisteredSetting() {
        String stringName = uniqueName("resetAllString");
        String intName = uniqueName("resetAllInt");
        preferences.register(stringName, String.class, "default", Preferences.Visibility.GLOBAL);
        preferences.register(intName, Integer.TYPE, 0, Preferences.Visibility.PROJECT);
        preferences.set(stringName, "changed");
        preferences.set(intName, 9);

        preferences.resetAll();

        assertEquals("default", preferences.get(stringName));
        assertEquals(0, preferences.<Integer>get(intName));
        assertEquals("\"default\"", persistence.globalStore.get(stringName));
        assertEquals("0", persistence.projectStore.get(intName));
    }

    @Test
    void resetGivesAFreshCopyOfAMutableDefaultNotTheSharedInstance() {
        Type mapType = new TypeToken<HashMap<String, String>>() {
        }.getType();
        String name = uniqueName("resetClone");
        HashMap<String, String> defaultMap = new HashMap<>();
        defaultMap.put("key", "defaultValue");
        preferences.register(name, mapType, defaultMap, Preferences.Visibility.GLOBAL);

        HashMap<String, String> changedMap = new HashMap<>();
        changedMap.put("key", "changedValue");
        preferences.set(name, changedMap);
        preferences.reset(name);

        HashMap<String, String> afterReset = preferences.get(name);
        assertEquals(defaultMap, afterReset);
        assertNotSame(defaultMap, afterReset, "reset must clone the default, not hand out the registered instance");
    }

    @Test
    void theResetWorkaroundLoopClearsValuesThroughPlainSetCalls() {
        // mirrors BurpExtensionSharedParameters.resetAllSettings(): the workaround writes
        // "" for String settings and null for everything else instead of calling resetAll()
        String stringName = uniqueName("workaroundString");
        String intName = uniqueName("workaroundInt");
        preferences.register(stringName, String.class, "default", Preferences.Visibility.GLOBAL);
        preferences.register(intName, Integer.TYPE, 3, Preferences.Visibility.GLOBAL);
        preferences.set(stringName, "changed");
        preferences.set(intName, 9);

        HashMap<String, Preferences.Visibility> registeredSettings = preferences.getRegisteredSettings();
        for (String item : registeredSettings.keySet()) {
            if (preferences.getType(item) == String.class)
                preferences.set(item, "");
            else
                preferences.set(item, null);
        }

        assertEquals("", preferences.get(stringName));
        assertNull(preferences.get(intName));
        assertEquals("\"\"", persistence.globalStore.get(stringName));
        assertEquals("null", persistence.globalStore.get(intName));
    }
}
