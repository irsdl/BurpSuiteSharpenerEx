// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import com.coreyd97.BurpExtenderUtilities.DefaultGsonProvider;
import com.coreyd97.BurpExtenderUtilities.PreferenceListener;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static ninja.burpsuite.libs.thirdparty.burpextenderutilities.InMemoryPersistence.uniqueName;
import static org.junit.jupiter.api.Assertions.*;

// Characterizes the listener notifications of library commit b7faf563.
public class PreferencesListenerTest {

    private record Notification(Object source, String settingName, Object newValue) {
    }

    private InMemoryPersistence persistence;
    private Preferences preferences;
    private final List<Notification> notifications = new ArrayList<>();
    private final PreferenceListener listener =
            (source, settingName, newValue) -> notifications.add(new Notification(source, settingName, newValue));

    @BeforeEach
    void setUp() {
        persistence = new InMemoryPersistence();
        preferences = new Preferences(persistence.montoyaApi(), new DefaultGsonProvider());
    }

    @Test
    void listenerFiresOnSetWithTheGivenEventSource() {
        String name = uniqueName("listenerSet");
        preferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL);
        preferences.addSettingListener(listener);

        Object eventSource = new Object();
        preferences.set(name, "changed", eventSource);

        assertEquals(List.of(new Notification(eventSource, name, "changed")), notifications);

        // without an explicit source, the Preferences instance itself is the source
        preferences.set(name, "changedAgain");
        assertEquals(2, notifications.size());
        assertSame(preferences, notifications.get(1).source());
    }

    @Test
    void resetNotifiesListenersTwice() {
        // reset() calls set() (first notification) and then notifies again itself;
        // callers must tolerate the duplicate
        String name = uniqueName("listenerReset");
        preferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL);
        preferences.set(name, "changed");
        preferences.addSettingListener(listener);

        preferences.reset(name);

        assertEquals(2, notifications.size());
        for (Notification notification : notifications) {
            assertSame(preferences, notification.source());
            assertEquals(name, notification.settingName());
            assertEquals("default", notification.newValue());
        }
    }

    @Test
    void removedListenersAreNotNotified() {
        String name = uniqueName("listenerRemove");
        preferences.register(name, String.class, "default", Preferences.Visibility.GLOBAL);
        preferences.addSettingListener(listener);
        preferences.set(name, "first");
        assertEquals(1, notifications.size());

        preferences.removeSettingListener(listener);
        preferences.set(name, "second");

        assertEquals(1, notifications.size());
    }
}
