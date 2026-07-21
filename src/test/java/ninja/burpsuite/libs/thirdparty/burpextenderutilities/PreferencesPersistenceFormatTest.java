// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import com.coreyd97.BurpExtenderUtilities.DefaultGsonProvider;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.google.gson.reflect.TypeToken;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.lang.reflect.Type;
import java.util.HashMap;

import static ninja.burpsuite.libs.thirdparty.burpextenderutilities.InMemoryPersistence.uniqueName;
import static org.junit.jupiter.api.Assertions.*;

// Golden JSON tests: they assert the EXACT strings written to the Burp stores for the
// setting shapes Sharpener really persists. Users' saved settings (sub-tab styles, flags,
// debug level) are stored in this format, so any drift here would lose user data on
// upgrade. Do not update the expected strings unless a migration is shipped with them.
public class PreferencesPersistenceFormatTest {

    private static final Type TAB_FEATURES_MAP_TYPE = new TypeToken<HashMap<String, TabFeaturesObject>>() {
    }.getType();

    private InMemoryPersistence persistence;
    private DefaultGsonProvider gsonProvider;
    private Preferences preferences;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryPersistence();
        gsonProvider = new DefaultGsonProvider();
        preferences = new Preferences(persistence.montoyaApi(), gsonProvider);
    }

    @Test
    void stringSettingIsStoredAsAQuotedJsonString() {
        String name = uniqueName("fmtString");
        preferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL);

        preferences.set(name, "test");
        assertEquals("\"test\"", persistence.globalStore.get(name));

        preferences.set(name, "");
        assertEquals("\"\"", persistence.globalStore.get(name));
    }

    @Test
    void primitiveSettingsAreStoredAsBareJsonLiterals() {
        // the debugLevel pattern from BurpExtensionSharedParameters
        String intName = uniqueName("fmtInt");
        preferences.register(intName, Integer.TYPE, 0, Preferences.Visibility.GLOBAL);
        assertEquals("0", persistence.globalStore.get(intName));

        // the isScrollable / mouseWheelToScroll pattern from SubTabsSettingsV2
        String boolName = uniqueName("fmtBool");
        preferences.register(boolName, Boolean.TYPE, false, Preferences.Visibility.GLOBAL);
        assertEquals("false", persistence.globalStore.get(boolName));
        preferences.set(boolName, true);
        assertEquals("true", persistence.globalStore.get(boolName));

        String doubleName = uniqueName("fmtDouble");
        preferences.register(doubleName, Double.class, 1.5, Preferences.Visibility.GLOBAL);
        assertEquals("1.5", persistence.globalStore.get(doubleName));
    }

    @Test
    void nullDefaultIsStoredAsTheJsonNullLiteral() {
        // SubTabsSettingsV2 registers its maps with a null default
        String name = uniqueName("fmtNull");

        preferences.register(name, TAB_FEATURES_MAP_TYPE, null, Preferences.Visibility.PROJECT);

        assertEquals("null", persistence.projectStore.get(name));
        assertNull(preferences.get(name));
    }

    @Test
    void byteArrayIsStoredAsABase64JsonString() {
        String name = uniqueName("fmtBytes");
        preferences.register(name, byte[].class, new byte[]{1, 2, 3}, Preferences.Visibility.GLOBAL);

        assertEquals("\"AQID\"", persistence.globalStore.get(name));
    }

    @Test
    void subTabStylesMapIsStoredWithTheExactKnownJsonLayout() {
        // mirrors the "TabFeaturesObject_Array_<tool>" settings of SubTabsSettingsV2,
        // the highest-value stored data: users' renamed and styled sub-tabs
        String name = uniqueName("fmtTabFeatures");
        preferences.register(name, TAB_FEATURES_MAP_TYPE, null, Preferences.Visibility.PROJECT);

        HashMap<String, TabFeaturesObject> map = new HashMap<>();
        map.put("my tab", sampleTabFeaturesObject());
        preferences.set(name, map);

        assertEquals("{\"my tab\":{\"index\":2,\"title\":\"My Tab\",\"tfoTitle\":\"my tab\","
                        + "\"titleHistory\":[\"Old Tab\"],\"name\":\"\",\"fontName\":\"Arial\",\"fontSize\":14.0,"
                        + "\"isBold\":true,\"isItalic\":false,\"isCloseButtonVisible\":false,"
                        + "\"iconResourceString\":\"star.png\",\"iconSize\":16,\"colorCode\":\"#ff0000\"}}",
                persistence.projectStore.get(name));
    }

    @Test
    void storedJsonRoundTripsBackToEqualObjects() {
        HashMap<String, TabFeaturesObject> map = new HashMap<>();
        map.put("my tab", sampleTabFeaturesObject());
        String mapJson = gsonProvider.getGson().toJson(map, TAB_FEATURES_MAP_TYPE);
        HashMap<String, TabFeaturesObject> reloadedMap = gsonProvider.getGson().fromJson(mapJson, TAB_FEATURES_MAP_TYPE);
        assertEquals(map, reloadedMap);
        assertEquals("#ff0000", reloadedMap.get("my tab").colorCode);

        byte[] bytes = new byte[]{1, 2, 3};
        assertArrayEquals(bytes, gsonProvider.getGson().fromJson(gsonProvider.getGson().toJson(bytes), byte[].class));

        assertEquals("test", gsonProvider.getGson().fromJson(gsonProvider.getGson().toJson("test"), String.class));
        assertEquals(0, gsonProvider.getGson().fromJson(gsonProvider.getGson().toJson(0), Integer.TYPE));
    }

    @Test
    void globalAndProjectSettingsLandOnlyInTheirOwnStores() {
        String globalName = uniqueName("fmtRoutingGlobal");
        String projectName = uniqueName("fmtRoutingProject");

        preferences.register(globalName, String.class, "globalValue", Preferences.Visibility.GLOBAL);
        preferences.register(projectName, String.class, "projectValue", Preferences.Visibility.PROJECT);

        assertTrue(persistence.globalStore.containsKey(globalName));
        assertFalse(persistence.globalStore.containsKey(projectName));
        assertTrue(persistence.projectStore.containsKey(projectName));
        assertFalse(persistence.projectStore.containsKey(globalName));
    }

    private static TabFeaturesObject sampleTabFeaturesObject() {
        return new TabFeaturesObject(2, "My Tab", new String[]{"Old Tab"}, "Arial", 14.0f,
                true, false, false, new Color(255, 0, 0), "star.png", 16);
    }
}
