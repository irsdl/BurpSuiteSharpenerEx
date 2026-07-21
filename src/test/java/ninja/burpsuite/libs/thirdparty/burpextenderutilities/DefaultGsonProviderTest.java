// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import com.coreyd97.BurpExtenderUtilities.DefaultGsonProvider;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.coreyd97.BurpExtenderUtilities.TypeAdapter.ByteArrayToBase64TypeAdapter;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

// Characterizes the gson provider wiring that Preferences relies on.
public class DefaultGsonProviderTest {

    @Test
    void registeringATypeAdapterRebuildsTheGsonInstance() {
        DefaultGsonProvider provider = new DefaultGsonProvider();
        Gson before = provider.getGson();
        assertEquals("[1,2,3]", before.toJson(new byte[]{1, 2, 3}));

        provider.registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter());

        Gson after = provider.getGson();
        assertNotSame(before, after);
        assertEquals("\"AQID\"", after.toJson(new byte[]{1, 2, 3}));
    }

    @Test
    void preferencesConstructionActivatesTheByteArrayAndAtomicIntegerAdapters() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        DefaultGsonProvider provider = new DefaultGsonProvider();
        new Preferences(persistence.montoyaApi(), provider);

        assertEquals("\"AQID\"", provider.getGson().toJson(new byte[]{1, 2, 3}));
        assertArrayEquals(new byte[]{1, 2, 3}, provider.getGson().fromJson("\"AQID\"", byte[].class));

        // upstream quirk kept as-is: serializing an AtomicInteger also increments it and
        // deserializing decrements, so a save/load pair cancels out; Sharpener stores no
        // AtomicInteger settings, so this stays inert
        AtomicInteger counter = new AtomicInteger(5);
        assertEquals("6", provider.getGson().toJson(counter));
        assertEquals(6, counter.get());
        assertEquals(5, provider.getGson().fromJson("6", AtomicInteger.class).get());
    }
}
