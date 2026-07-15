// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.generic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

// Guards the duplicate extension load detection: a second live copy must be
// detected, while a dead marker left by a quick unload plus reload must not
// trigger a false positive (that false positive is why the old menu bar based
// check was removed in version 4.6).
public class SingleInstanceGuardTest {

    private static final String KEY = "test.instanceMarker";
    private JPanel host;

    @BeforeEach
    void setUp() {
        host = new JPanel();
    }

    @Test
    void noMarkerMeansNoLiveInstance() {
        assertFalse(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }

    @Test
    void liveMarkerIsDetected() {
        SingleInstanceGuard.publish(host, KEY, () -> true);
        assertTrue(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }

    @Test
    void deadMarkerIsNotDetected() {
        // the quick unload plus reload race: the marker exists but its owner is unloaded
        SingleInstanceGuard.publish(host, KEY, () -> false);
        assertFalse(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }

    @Test
    void throwingMarkerIsNotDetected() {
        // a half dead owner must count as not loaded, never break the new copy's load
        SingleInstanceGuard.publish(host, KEY, () -> {
            throw new IllegalStateException("owner is gone");
        });
        assertFalse(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }

    @Test
    void foreignValueUnderTheKeyIsIgnored() {
        host.putClientProperty(KEY, "not a marker");
        assertFalse(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }

    @Test
    void markerTurnsDeadWhenItsRunnerStops() {
        // the real marker is backed by DelayedTaskRunner.isStopped(), which flips
        // as soon as unload starts, so a reloading copy never sees itself as live
        DelayedTaskRunner runner = new DelayedTaskRunner();
        SingleInstanceGuard.publish(host, KEY, () -> !runner.isStopped());
        assertTrue(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));

        runner.stop();
        assertFalse(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }

    @Test
    void publishReplacesADeadLeftoverMarker() {
        SingleInstanceGuard.publish(host, KEY, () -> false);
        SingleInstanceGuard.publish(host, KEY, () -> true);
        assertTrue(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }

    @Test
    void clearRemovesOwnMarker() {
        BooleanSupplier ownMarker = () -> true;
        SingleInstanceGuard.publish(host, KEY, ownMarker);
        SingleInstanceGuard.clear(host, KEY, ownMarker);

        assertNull(host.getClientProperty(KEY), "the marker must be removed so it cannot leak the classloader");
        assertFalse(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }

    @Test
    void clearKeepsANewerCopysMarker() {
        // an old copy unloading late must never remove the marker the new copy published
        BooleanSupplier oldMarker = () -> false;
        BooleanSupplier newMarker = () -> true;
        SingleInstanceGuard.publish(host, KEY, oldMarker);
        SingleInstanceGuard.publish(host, KEY, newMarker);

        SingleInstanceGuard.clear(host, KEY, oldMarker);

        assertSame(newMarker, host.getClientProperty(KEY));
        assertTrue(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }

    @Test
    void clearWithNullOwnMarkerDoesNothing() {
        // a copy that never published (for example a detected duplicate) clears nothing
        SingleInstanceGuard.publish(host, KEY, () -> true);
        SingleInstanceGuard.clear(host, KEY, null);
        assertTrue(SingleInstanceGuard.isAnotherInstanceLive(host, KEY));
    }
}
