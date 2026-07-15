// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.function.Consumer;

// A named mouse wheel listener type used by this extension.
// Listeners of this type can be found and removed with instanceof,
// so listeners owned by Burp or the look and feel are never touched.
public class MouseWheelListenerExtensionHandler implements MouseWheelListener {

    private final Consumer<MouseWheelEvent> mouseWheelEventConsumer;

    public MouseWheelListenerExtensionHandler(Consumer<MouseWheelEvent> consumer) {
        this.mouseWheelEventConsumer = consumer;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        this.mouseWheelEventConsumer.accept(e);
    }
}
