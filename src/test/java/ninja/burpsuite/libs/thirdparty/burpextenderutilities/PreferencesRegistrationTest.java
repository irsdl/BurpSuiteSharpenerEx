// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import ninja.burpsuite.libs.thirdparty.burpextenderutilities.nameManager.NameCollisionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static ninja.burpsuite.libs.thirdparty.burpextenderutilities.InMemoryPersistence.uniqueName;
import static org.junit.jupiter.api.Assertions.*;

// Characterization tests for the preferences library used by every *Settings class.
// They define the registration contract the vendored copy must keep unchanged.
public class PreferencesRegistrationTest {

    private InMemoryPersistence persistence;
    private Preferences preferences;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryPersistence();
        preferences = new Preferences(persistence.montoyaApi(), new DefaultGsonProvider());
    }

    @Test
    void registerGlobalPersistsTheDefaultToTheGlobalStore() {
        String name = uniqueName("regGlobal");

        preferences.register(name, String.class, "test", Preferences.Visibility.GLOBAL);

        assertEquals("\"test\"", persistence.globalStore.get(name));
        assertFalse(persistence.projectStore.containsKey(name));
        assertEquals("test", preferences.get(name));
    }

    @Test
    void registerProjectPersistsTheDefaultToTheProjectStoreOnly() {
        String name = uniqueName("regProject");

        preferences.register(name, String.class, "test", Preferences.Visibility.PROJECT);

        assertEquals("\"test\"", persistence.projectStore.get(name));
        assertFalse(persistence.globalStore.containsKey(name));
        assertEquals("test", preferences.get(name));
    }

    @Test
    void registerVolatilePersistsNothing() {
        String name = uniqueName("regVolatile");

        preferences.register(name, String.class, "test", Preferences.Visibility.VOLATILE);

        assertFalse(persistence.globalStore.containsKey(name));
        assertFalse(persistence.projectStore.containsKey(name));
        assertEquals("test", preferences.get(name));
    }

    @Test
    void registerWithoutPersistDefaultLeavesTheStoreUntouched() {
        String name = uniqueName("regNoPersist");

        preferences.register(name, String.class, "test", Preferences.Visibility.GLOBAL, false);

        assertFalse(persistence.globalStore.containsKey(name));
        assertEquals("test", preferences.get(name));
    }

    @Test
    void registerLoadsAPreviouslyStoredValueInsteadOfTheDefault() {
        // simulates an upgrade: the store already holds a value saved by an earlier session
        String name = uniqueName("regStored");
        persistence.globalStore.put(name, "\"stored\"");

        preferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL);

        assertEquals("stored", preferences.get(name));
        assertEquals("\"stored\"", persistence.globalStore.get(name));
    }

    @Test
    void registerFallsBackToTheDefaultWhenStoredJsonIsCorrupt() {
        String name = uniqueName("regCorrupt");
        persistence.globalStore.put(name, "{corrupt");

        preferences.register(name, Integer.class, 5, Preferences.Visibility.GLOBAL);

        assertEquals(5, preferences.<Integer>get(name));
        assertEquals("5", persistence.globalStore.get(name));
    }

    @Test
    void registeringTheSameNameTwiceThrowsNameCollision() {
        String name = uniqueName("regDuplicate");
        preferences.register(name, String.class, "one", Preferences.Visibility.GLOBAL);

        assertThrows(NameCollisionException.class,
                () -> preferences.register(name, String.class, "two", Preferences.Visibility.GLOBAL));
    }

    @Test
    void unregisterRemovesTheStoredValueAndFreesTheName() {
        String name = uniqueName("regUnregister");
        preferences.register(name, String.class, "one", Preferences.Visibility.GLOBAL);
        preferences.set(name, "changed");

        preferences.unregister(name);

        assertFalse(persistence.globalStore.containsKey(name));
        assertThrows(UnmanagedSettingException.class, () -> preferences.get(name));

        preferences.register(name, String.class, "again", Preferences.Visibility.GLOBAL);
        assertEquals("again", preferences.get(name));
    }

    @Test
    void typeAndRegisteredSettingsReflectRegistrations() {
        String name = uniqueName("regMeta");

        preferences.register(name, Integer.TYPE, 0, Preferences.Visibility.PROJECT);

        assertEquals(Integer.TYPE, preferences.getType(name));
        assertEquals(Preferences.Visibility.PROJECT, preferences.getRegisteredSettings().get(name));
    }

    @Test
    void gettingOrSettingAnUnknownNameThrowsUnmanagedSetting() {
        String name = uniqueName("regUnknown");

        UnmanagedSettingException getError = assertThrows(UnmanagedSettingException.class, () -> preferences.get(name));
        // the exact wording matters: ExtendedPreferences matches on the old
        // "has not been registered" text, so its auto-register fallback never triggers
        assertTrue(getError.getMessage().contains("is not managed by this Preferences instance"));

        assertThrows(UnmanagedSettingException.class, () -> preferences.set(name, "value"));
    }
}
