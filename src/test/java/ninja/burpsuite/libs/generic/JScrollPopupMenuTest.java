// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class JScrollPopupMenuTest {

    private static Component fixedHeightComponent(int height) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(100, height));
        return panel;
    }

    @Test
    void countRowsThatFitCountsOnlyFullRows() {
        Component[] components = {
                fixedHeightComponent(40),
                fixedHeightComponent(40),
                fixedHeightComponent(40)
        };

        assertEquals(3, JScrollPopupMenu.countRowsThatFit(components, 120));
        assertEquals(2, JScrollPopupMenu.countRowsThatFit(components, 119));
        assertEquals(2, JScrollPopupMenu.countRowsThatFit(components, 80));
        assertEquals(0, JScrollPopupMenu.countRowsThatFit(components, 39));
    }

    @Test
    void countRowsThatFitIgnoresTheScrollBar() {
        Component[] components = {
                new JScrollBar(),
                fixedHeightComponent(40),
                fixedHeightComponent(40)
        };

        assertEquals(2, JScrollPopupMenu.countRowsThatFit(components, 80));
    }

    @Test
    void countRowsThatFitHandlesEmptyInput() {
        assertEquals(0, JScrollPopupMenu.countRowsThatFit(new Component[0], 100));
    }

    @Test
    void scrollBarStaysHiddenWhenRowsAreNotCapped() {
        JScrollPopupMenu popupMenu = new JScrollPopupMenu();
        popupMenu.setMaximumVisibleRows(Integer.MAX_VALUE);
        for (int i = 0; i < 30; i++) {
            popupMenu.add(new JMenuItem("item " + i));
        }

        // one scrollbar plus the items
        assertEquals(31, popupMenu.getComponentCount());
        boolean scrollBarVisible = false;
        for (Component comp : popupMenu.getComponents()) {
            if (comp instanceof JScrollBar scrollBar) {
                scrollBarVisible = scrollBar.isVisible();
            }
        }
        assertFalse(scrollBarVisible, "The scrollbar must stay hidden until the menu does not fit the screen");
    }
}
