// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.burp.generic;

import com.coreyd97.BurpExtenderUtilities.DefaultGsonProvider;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.InMemoryPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static ninja.burpsuite.libs.thirdparty.burpextenderutilities.InMemoryPersistence.uniqueName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

// Characterizes the safe wrappers every Sharpener settings class goes through.
public class ExtendedPreferencesTest {

    private InMemoryPersistence persistence;
    private ExtendedPreferences preferences;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryPersistence();
        preferences = new ExtendedPreferences(persistence.montoyaApi(), new DefaultGsonProvider());
        preferences.sharedParameters = mockSharedParameters();
    }

    // the safe methods read sharedParameters.debugLevel directly, so the mock needs a real value
    private static BurpExtensionSharedParameters mockSharedParameters() {
        BurpExtensionSharedParameters sharedParameters = mock(BurpExtensionSharedParameters.class);
        sharedParameters.debugLevel = BurpExtensionSharedParameters.DebugLevels.None.getValue();
        return sharedParameters;
    }

    @Test
    void safeGettersReturnTheirDefaultsForUnregisteredNamesWithoutThrowing() {
        assertEquals("", preferences.safeGetStringSetting(uniqueName("safeString")));
        assertFalse(preferences.safeGetBooleanSetting(uniqueName("safeBoolean")));
        assertEquals(-1, preferences.safeGetIntSetting(uniqueName("safeInt")));
        assertEquals("fallback", preferences.safeGetSetting(uniqueName("safeDefault"), "fallback"));
        assertNull(preferences.safeGetSetting(uniqueName("safeNull")));
    }

    @Test
    void safeGettersReturnTheRealValuesForRegisteredNames() {
        String stringName = uniqueName("safeRegString");
        String booleanName = uniqueName("safeRegBoolean");
        String intName = uniqueName("safeRegInt");
        preferences.register(stringName, String.class, "value", Preferences.Visibility.GLOBAL);
        preferences.register(booleanName, Boolean.TYPE, true, Preferences.Visibility.GLOBAL);
        preferences.register(intName, Integer.TYPE, 7, Preferences.Visibility.GLOBAL);

        assertEquals("value", preferences.safeGetStringSetting(stringName));
        assertTrue(preferences.safeGetBooleanSetting(booleanName));
        assertEquals(7, preferences.safeGetIntSetting(intName));
        assertEquals("value", preferences.safeGetSetting(stringName, "fallback"));
    }

    @Test
    void safeSetSettingWritesAndVerifiesARegisteredSetting() {
        String name = uniqueName("safeSet");
        preferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL);

        preferences.safeSetSetting(name, "changed", Preferences.Visibility.GLOBAL);

        assertEquals("changed", preferences.get(name));
        assertEquals("\"changed\"", persistence.globalStore.get(name));
    }

    @Test
    void safeSetSettingOnAnUnregisteredNameFailsSilentlyWithoutRegistering() {
        // Characterization of the dead auto-register fallback: the code matches the old
        // "has not been registered" message, but library commit b7faf563 throws
        // UnmanagedSettingException saying "is not managed by this Preferences instance".
        // The fallback therefore never runs and the call gives up after 3 tries.
        // A follow-up change should detect the exception by type and flip these assertions.
        String name = uniqueName("safeSetUnregistered");

        assertDoesNotThrow(() -> preferences.safeSetSetting(name, "value", Preferences.Visibility.GLOBAL));

        assertFalse(persistence.globalStore.containsKey(name));
        assertFalse(preferences.getRegisteredSettings().containsKey(name));
        assertEquals("", preferences.safeGetStringSetting(name));
    }

    @Test
    void safeSetSettingWithNullStillPersistsTheNull() {
        // StandardSettings.resetSettings relies on this path: the null value is saved,
        // then the verify step trips over null.equals and the retries do nothing more
        String name = uniqueName("safeSetNull");
        preferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL);

        assertDoesNotThrow(() -> preferences.safeSetSetting(name, null, Preferences.Visibility.GLOBAL));

        assertNull(preferences.get(name));
        assertEquals("null", persistence.globalStore.get(name));
    }

    @Test
    void safeSetSettingRetriesAfterATransientFailure() {
        var flakyPreferences = new ExtendedPreferences(persistence.montoyaApi(), new DefaultGsonProvider()) {
            int remainingFailures = 1;

            @Override
            public void set(String settingName, Object value, Object eventSource) {
                if (remainingFailures-- > 0)
                    throw new RuntimeException("Temporary save failure");
                super.set(settingName, value, eventSource);
            }
        };
        flakyPreferences.sharedParameters = mockSharedParameters();
        String name = uniqueName("safeSetRetry");
        // persistDefault=false so registration does not call set() and eat the injected failure
        flakyPreferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL, false);

        flakyPreferences.safeSetSetting(name, "saved", Preferences.Visibility.GLOBAL);

        assertEquals("saved", flakyPreferences.get(name));
        assertEquals("\"saved\"", persistence.globalStore.get(name));
    }
}
