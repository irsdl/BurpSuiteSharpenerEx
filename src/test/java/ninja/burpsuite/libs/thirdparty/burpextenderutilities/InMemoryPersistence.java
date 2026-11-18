// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Persistence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Fake Burp persistence so the preferences code can run headless.
// The global store fakes montoyaApi.persistence().preferences() (Burp user-level store) and
// the project store fakes montoyaApi.persistence().extensionData() (project file store).
// Tests can read the maps directly to assert the exact stored JSON, or seed them to
// simulate values saved by an earlier session.
//
// Static-state rule: the preferences library keeps registered setting names in a static
// NameManager shared by the whole JVM, so it is NOT reset between tests. Every test must
// use uniqueName() for each setting it registers, otherwise later tests (or later runs of
// the same test class in one JVM) fail with a name collision.
public final class InMemoryPersistence {

    public final Map<String, String> globalStore = new LinkedHashMap<>();
    public final Map<String, String> projectStore = new LinkedHashMap<>();

    private final MontoyaApi montoyaApi;

    private static final AtomicLong NAME_COUNTER = new AtomicLong();

    public InMemoryPersistence() {
        burp.api.montoya.persistence.Preferences preferencesStore = mock(burp.api.montoya.persistence.Preferences.class);
        when(preferencesStore.getString(anyString())).thenAnswer(invocation -> globalStore.get((String) invocation.getArgument(0)));
        doAnswer(invocation -> globalStore.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(preferencesStore).setString(anyString(), anyString());
        doAnswer(invocation -> globalStore.remove((String) invocation.getArgument(0)))
                .when(preferencesStore).deleteString(anyString());

        PersistedObject extensionData = mock(PersistedObject.class);
        when(extensionData.getString(anyString())).thenAnswer(invocation -> projectStore.get((String) invocation.getArgument(0)));
        doAnswer(invocation -> projectStore.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(extensionData).setString(anyString(), anyString());
        doAnswer(invocation -> projectStore.remove((String) invocation.getArgument(0)))
                .when(extensionData).deleteString(anyString());

        Persistence persistence = mock(Persistence.class);
        when(persistence.preferences()).thenReturn(preferencesStore);
        when(persistence.extensionData()).thenReturn(extensionData);

        montoyaApi = mock(MontoyaApi.class);
        when(montoyaApi.persistence()).thenReturn(persistence);
        when(montoyaApi.logging()).thenReturn(mock(Logging.class));
    }

    public MontoyaApi montoyaApi() {
        return montoyaApi;
    }

    // returns a setting name that no other test in this JVM has used
    public static String uniqueName(String base) {
        return base + "_" + NAME_COUNTER.incrementAndGet();
    }
}
