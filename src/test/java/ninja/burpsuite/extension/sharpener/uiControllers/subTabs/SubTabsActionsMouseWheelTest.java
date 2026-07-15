// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import ninja.burpsuite.libs.generic.MouseWheelListenerExtensionHandler;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

// Memory leak regression tests for the "mouse wheel to scroll tabs" feature.
// The old code removed the LAST wheel listener from the tabbed pane on unload.
// The look and feel can register its own wheel listener on the pane, so the old
// code could remove that one instead and leave the Sharpener listener behind.
public class SubTabsActionsMouseWheelTest {

    private static long countExtensionHandlers(JTabbedPane tabbedPane) {
        long count = 0;
        for (MouseWheelListener listener : tabbedPane.getMouseWheelListeners()) {
            if (listener instanceof MouseWheelListenerExtensionHandler) {
                count++;
            }
        }
        return count;
    }

    @Test
    void attachAddsOneIdentifiableHandler() {
        JTabbedPane tabbedPane = new JTabbedPane();

        SubTabsActions.attachMouseWheelHandler(tabbedPane, e -> {
        });

        assertEquals(1, countExtensionHandlers(tabbedPane));
    }

    @Test
    void attachTwiceDoesNotStackHandlers() {
        JTabbedPane tabbedPane = new JTabbedPane();

        SubTabsActions.attachMouseWheelHandler(tabbedPane, e -> {
        });
        SubTabsActions.attachMouseWheelHandler(tabbedPane, e -> {
        });

        assertEquals(1, countExtensionHandlers(tabbedPane));
    }

    @Test
    void detachRemovesOnlyTheExtensionHandler() {
        JTabbedPane tabbedPane = new JTabbedPane();
        // stands in for a wheel listener owned by Burp or the look and feel
        MouseWheelListener foreignListener = e -> {
        };
        tabbedPane.addMouseWheelListener(foreignListener);

        SubTabsActions.attachMouseWheelHandler(tabbedPane, e -> {
        });
        SubTabsActions.detachMouseWheelHandlers(tabbedPane);

        assertEquals(0, countExtensionHandlers(tabbedPane));
        assertArrayEquals(new MouseWheelListener[]{foreignListener}, tabbedPane.getMouseWheelListeners());
    }

    @Test
    void detachWithoutAttachKeepsForeignListeners() {
        JTabbedPane tabbedPane = new JTabbedPane();
        MouseWheelListener foreignListener = e -> {
        };
        tabbedPane.addMouseWheelListener(foreignListener);

        SubTabsActions.detachMouseWheelHandlers(tabbedPane);

        assertArrayEquals(new MouseWheelListener[]{foreignListener}, tabbedPane.getMouseWheelListeners());
    }

    @Test
    void attachedHandlerForwardsWheelEvents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        AtomicInteger receivedEvents = new AtomicInteger();

        SubTabsActions.attachMouseWheelHandler(tabbedPane, e -> receivedEvents.incrementAndGet());

        MouseWheelEvent wheelEvent = new MouseWheelEvent(tabbedPane, MouseWheelEvent.MOUSE_WHEEL,
                System.currentTimeMillis(), 0, 1, 1, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1);
        for (MouseWheelListener listener : tabbedPane.getMouseWheelListeners()) {
            if (listener instanceof MouseWheelListenerExtensionHandler) {
                listener.mouseWheelMoved(wheelEvent);
            }
        }

        assertEquals(1, receivedEvents.get());
    }
}
