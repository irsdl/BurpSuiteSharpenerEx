// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import ninja.burpsuite.libs.thirdparty.burpextenderutilities.nameManager.NameManager;
import com.google.gson.reflect.TypeToken;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.lang.reflect.Type;
import java.util.HashMap;

import static ninja.burpsuite.libs.thirdparty.burpextenderutilities.InMemoryPersistence.uniqueName;
import static org.junit.jupiter.api.Assertions.*;

// Set/get round trips plus the reload scenario: a new Preferences instance over the same
// Burp stores must load what the previous instance saved. This mirrors an extension
// unload/reload and a Burp restart.
public class PreferencesRoundTripAndReloadTest {

    private static final Type TAB_FEATURES_MAP_TYPE = new TypeToken<HashMap<String, TabFeaturesObject>>() {
    }.getType();

    private InMemoryPersistence persistence;
    private Preferences preferences;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryPersistence();
        preferences = new Preferences(persistence.montoyaApi(), new DefaultGsonProvider());
    }

    @Test
    void setThenGetReturnsTheNewValueForEachVisibility() {
        for (Preferences.Visibility visibility : Preferences.Visibility.values()) {
            String name = uniqueName("roundTrip" + visibility);
            preferences.register(name, String.class, "default", visibility);

            preferences.set(name, "changed");

            assertEquals("changed", preferences.get(name), "visibility: " + visibility);
            assertEquals(visibility == Preferences.Visibility.GLOBAL, persistence.globalStore.containsKey(name),
                    "global store routing for " + visibility);
            assertEquals(visibility == Preferences.Visibility.PROJECT, persistence.projectStore.containsKey(name),
                    "project store routing for " + visibility);
        }
    }

    @Test
    void aFreshInstanceOverTheSameStoresLoadsThePreviouslySavedValues() {
        String stringName = uniqueName("reloadString");
        String mapName = uniqueName("reloadMap");

        preferences.register(stringName, String.class, "default", Preferences.Visibility.GLOBAL);
        preferences.set(stringName, "changed");

        preferences.register(mapName, TAB_FEATURES_MAP_TYPE, null, Preferences.Visibility.PROJECT);
        HashMap<String, TabFeaturesObject> map = new HashMap<>();
        TabFeaturesObject savedObject = new TabFeaturesObject(1, "Styled Tab", new String[]{}, "Consolas", 12.0f,
                false, true, false, new Color(0, 128, 255), "", 0);
        map.put(savedObject.getTfoTitle(), savedObject);
        preferences.set(mapName, map);

        // A real reload gives the extension a fresh classloader, so the static NameManager
        // starts empty. Release the names directly to simulate that; unregister() would
        // also delete the stored values, which is not what happens on a reload.
        NameManager.release(stringName);
        NameManager.release(mapName);

        Preferences reloaded = new Preferences(persistence.montoyaApi(), new DefaultGsonProvider());
        reloaded.register(stringName, String.class, "default", Preferences.Visibility.GLOBAL);
        reloaded.register(mapName, TAB_FEATURES_MAP_TYPE, null, Preferences.Visibility.PROJECT);

        assertEquals("changed", reloaded.get(stringName));
        HashMap<String, TabFeaturesObject> reloadedMap = reloaded.get(mapName);
        assertNotNull(reloadedMap);
        assertEquals(map, reloadedMap);
        TabFeaturesObject reloadedObject = reloadedMap.get("styled tab");
        assertEquals("Styled Tab", reloadedObject.getTitle());
        assertEquals("Consolas", reloadedObject.fontName);
        assertEquals(12.0f, reloadedObject.fontSize);
        assertFalse(reloadedObject.isBold);
        assertTrue(reloadedObject.isItalic);
        assertEquals("#0080ff", reloadedObject.colorCode);
    }
}
