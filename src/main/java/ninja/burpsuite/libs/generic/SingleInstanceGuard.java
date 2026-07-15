// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.generic;

import javax.swing.JComponent;
import java.util.function.BooleanSupplier;

// Detects a second live copy of the extension inside the same Burp process.
// A menu bar check cannot tell "stale leftover menu after a quick reload" from
// "a second copy is really loaded", so this guard uses a liveness marker instead.
// The marker is a client property on a Swing component every copy can reach
// (Burp's main frame root pane). Each extension copy has its own classloader,
// so only JDK types cross the boundary: the marker value is a BooleanSupplier
// (loaded by the bootstrap classloader) that reports whether its owner is still loaded.
public class SingleInstanceGuard {

    private SingleInstanceGuard() {
    }

    // True when a marker exists on the host and its owner reports it is still loaded.
    // A missing marker, a foreign value under the key, a supplier that returns false,
    // or a supplier that throws (its owner is half dead) all mean "no live instance".
    public static boolean isAnotherInstanceLive(JComponent host, String markerKey) {
        Object marker = host.getClientProperty(markerKey);
        if (marker instanceof BooleanSupplier ownerIsLoaded) {
            try {
                return ownerIsLoaded.getAsBoolean();
            } catch (Throwable e) {
                return false;
            }
        }
        return false;
    }

    // Publishes this copy's liveness marker, replacing any dead leftover marker.
    public static void publish(JComponent host, String markerKey, BooleanSupplier ownerIsLoaded) {
        host.putClientProperty(markerKey, ownerIsLoaded);
    }

    // Removes the marker, but only when it is still this copy's own marker.
    // Clearing must not remove a newer copy's marker, and it must always run on
    // unload: the marker holds a reference into the extension classloader, and a
    // leftover reference would keep the unloaded classloader alive.
    public static void clear(JComponent host, String markerKey, BooleanSupplier ownMarker) {
        if (ownMarker != null && host.getClientProperty(markerKey) == ownMarker) {
            host.putClientProperty(markerKey, null);
        }
    }
}
