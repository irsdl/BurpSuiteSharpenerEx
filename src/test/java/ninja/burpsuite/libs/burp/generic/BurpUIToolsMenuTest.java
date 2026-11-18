// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class BurpUIToolsMenuTest {
    @Test
    void subMenuLookupHandlesEmptyMenusSeparatorsNullTextAndTheLastItem() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JMenuBar bar = new JMenuBar();
            JMenu window = new JMenu("Window");
            bar.add(window);
            assertNull(BurpUITools.getSubMenuComponentFromMain("Window", "Reattach Repeater", bar));
            window.addSeparator();
            JMenuItem unnamed = new JMenuItem(); unnamed.setText(null); window.add(unnamed);
            JMenuItem last = new JMenuItem("Reattach Repeater"); window.add(last);
            assertSame(last, BurpUITools.getSubMenuComponentFromMain("Window", "reattach repeater", bar));
            assertNull(BurpUITools.getSubMenuComponentFromMain("Window", null, bar));
            assertFalse(BurpUITools.switchToMainTab("Repeater", null));
        });
    }

    @Test
    void findsMenusAmongNonMenuComponents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JMenuBar bar = new JMenuBar();
            bar.add(new JLabel("Burp"));
            bar.add(Box.createHorizontalGlue());
            JMenu unnamed = new JMenu();
            unnamed.setText(null);
            bar.add(unnamed);
            JMenu sharpener = new JMenu(" Sharpener ");
            bar.add(sharpener);
            bar.add(new JButton("Help"));

            assertTrue(BurpUITools.isMenuBarLoaded(" sharpENER ", bar));
            assertSame(sharpener, BurpUITools.getMenuItem("sharpener", bar));
            assertFalse(BurpUITools.isMenuBarLoaded("Missing", bar));
            assertNull(BurpUITools.getMenuItem("Help", bar));
        });
    }

    @Test
    void removesAllStaleMenusAndPreservesOtherComponents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JMenuBar bar = new JMenuBar();
            JLabel label = new JLabel("Burp");
            JMenu help = new JMenu("Help");
            bar.add(label);
            bar.add(new JMenu("Sharpener"));
            bar.add(new JMenu(" sharpENER "));
            bar.add(Box.createHorizontalGlue());
            bar.add(help);

            BurpUITools.removeMenuBarByName("Sharpener", bar, true);

            assertFalse(BurpUITools.isMenuBarLoaded("Sharpener", bar));
            assertEquals(3, bar.getComponentCount());
            assertSame(label, bar.getComponent(0));
            assertSame(help, BurpUITools.getMenuItem("Help", bar));
        });
    }

    @Test
    void missingMenuBarOrNameDoesNotThrow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(BurpUITools.isMenuBarLoaded("Sharpener", null));
            assertFalse(BurpUITools.isMenuBarLoaded("Sharpener", new JMenuBar()));
            assertNull(BurpUITools.getMenuItem(null, new JMenuBar()));
            BurpUITools.removeMenuBarByName("Sharpener", null, true);
            BurpUITools.removeMenuBarByName(null, new JMenuBar(), true);
        });
    }
}
