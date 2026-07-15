// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

// These tests build a small Swing tree that mimics the Burp Repeater tab bar:
// a tool JTabbedPane inside a main JTabbedPane, where each tab has a custom
// tab component (a JPanel) that holds a JTextField with the tab title.
//
// Burp 2026+ paints the tab title using the tabbed pane per-tab foreground colour
// (JTabbedPane.setForegroundAt), not the title field colour. The new-Burp tests
// below assert against getForegroundAt.
public class SubTabsContainerHandlerColorTest {

    private ExtensionSharedParameters sharedParameters;
    private JTabbedPane toolTabbedPane;
    private JTextField tabOneTitleField;

    @BeforeEach
    void setUp() {
        sharedParameters = mock(ExtensionSharedParameters.class);
        sharedParameters.allSubTabContainerHandlers = new HashMap<>();
        sharedParameters.allSubTabContainerHandlers.put(BurpUITools.MainTabs.Repeater, new ArrayList<>());
        sharedParameters.isTabGroupSupportedByDefault = true;
        sharedParameters.isDarkMode = false;

        JTabbedPane mainTabbedPane = new JTabbedPane();
        toolTabbedPane = new JTabbedPane();
        mainTabbedPane.addTab("Repeater", toolTabbedPane);

        tabOneTitleField = addSubTab(toolTabbedPane, "1");
        addSubTab(toolTabbedPane, "2");
    }

    private JTextField addSubTab(JTabbedPane tabbedPane, String title) {
        tabbedPane.addTab(title, new JPanel());
        JPanel tabComponent = new JPanel();
        JTextField titleTextField = new JTextField(title);
        tabComponent.add(titleTextField);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabComponent);
        return titleTextField;
    }

    private SubTabsContainerHandler createHandler(int tabIndex, boolean forComparison) {
        return new SubTabsContainerHandler(sharedParameters, toolTabbedPane, tabIndex, forComparison);
    }

    @Test
    void handlerFindsTheTabTitleTextField() {
        SubTabsContainerHandler handler = createHandler(0, true);
        assertTrue(handler.isValid());
        assertEquals(BurpUITools.MainTabs.Repeater, handler.currentToolTab);
        assertEquals(tabOneTitleField, handler.currentTabTextField);
        assertEquals("1", handler.getTabTitle());
    }

    @Test
    void newBurpSetColorSetsTheTabForegroundAndNotTheTabBackground() {
        sharedParameters.isTabTextColorSetByBackground = false;
        SubTabsContainerHandler handler = createHandler(0, true);

        handler.setColor(Color.RED, true);

        // Burp 2026+ paints the title from the tabbed pane per-tab foreground
        assertEquals(Color.RED, toolTabbedPane.getForegroundAt(0));
        assertEquals(Color.RED, handler.getColor());
        // the tab must not become a coloured background chip
        assertEquals(toolTabbedPane.getBackground(), toolTabbedPane.getBackgroundAt(0));
    }

    @Test
    void newBurpSetColorClearsAnOldTabBackground() {
        sharedParameters.isTabTextColorSetByBackground = false;
        SubTabsContainerHandler handler = createHandler(0, true);

        // simulate a leftover tab background from the old colouring method
        toolTabbedPane.setBackgroundAt(0, Color.MAGENTA);
        handler.setColor(Color.BLUE, true);

        assertEquals(Color.BLUE, toolTabbedPane.getForegroundAt(0));
        assertEquals(toolTabbedPane.getBackground(), toolTabbedPane.getBackgroundAt(0));
    }

    @Test
    void oldBurpSetColorStillUsesTabBackground() {
        sharedParameters.isTabTextColorSetByBackground = true;
        SubTabsContainerHandler handler = createHandler(0, true);
        Color originalForeground = tabOneTitleField.getForeground();

        handler.setColor(Color.RED, true);

        assertEquals(Color.RED, toolTabbedPane.getBackgroundAt(0));
        assertEquals(originalForeground, tabOneTitleField.getForeground());
    }

    @Test
    void nullColorIsIgnored() {
        sharedParameters.isTabTextColorSetByBackground = false;
        SubTabsContainerHandler handler = createHandler(0, true);
        Color before = toolTabbedPane.getForegroundAt(0);

        handler.setColor(null, true);

        // a null colour must not change the tab foreground
        assertEquals(before, toolTabbedPane.getForegroundAt(0));
    }

    @Test
    void isDefaultColourAcceptsKnownDefaultsInLightMode() {
        sharedParameters.isDarkMode = false;
        SubTabsContainerHandler handler = createHandler(0, true);

        assertTrue(handler.isDefaultColour(Color.decode("#000000")));
        assertTrue(handler.isDefaultColour(Color.decode("#010101")));
        assertFalse(handler.isDefaultColour(Color.RED));
        assertFalse(handler.isDefaultColour(null));
    }

    @Test
    void isDefaultColourAcceptsKnownDefaultsInDarkMode() {
        sharedParameters.isDarkMode = true;
        SubTabsContainerHandler handler = createHandler(0, true);

        assertTrue(handler.isDefaultColour(Color.decode("#bbbbbb")));
        assertTrue(handler.isDefaultColour(Color.decode("#bcbcbc")));
        assertFalse(handler.isDefaultColour(Color.decode("#000000")));
    }

    @Test
    void isDefaultColourAcceptsTheLookAndFeelDefaultColour() {
        SubTabsContainerHandler handler = createHandler(0, true);
        sharedParameters.defaultTabFeaturesObjectStyle = handler.getTabFeaturesObjectStyle();
        Color lookAndFeelDefault = sharedParameters.defaultTabFeaturesObjectStyle.getColor();

        assertTrue(handler.isDefaultColour(lookAndFeelDefault));
    }

    @Test
    void setToDefaultRestoresTheDefaultForegroundOnNewBurp() {
        sharedParameters.isTabTextColorSetByBackground = false;
        SubTabsContainerHandler handler = createHandler(0, true);
        sharedParameters.defaultTabFeaturesObjectStyle = handler.getTabFeaturesObjectStyle();
        Color defaultColour = sharedParameters.defaultTabFeaturesObjectStyle.getColor();

        handler.setColor(Color.RED, true);
        assertEquals(Color.RED, toolTabbedPane.getForegroundAt(0));
        assertFalse(handler.isDefault());

        handler.setToDefault(true);

        // new Burp applies the real default colour directly, no reset marker is used
        assertEquals(defaultColour, toolTabbedPane.getForegroundAt(0));
        assertEquals(toolTabbedPane.getBackground(), toolTabbedPane.getBackgroundAt(0));
        assertTrue(handler.isDefault());
    }

    @Test
    void setColorWithTabWatcherDoesNotLoopOnNewBurp() {
        sharedParameters.isTabTextColorSetByBackground = false;
        SubTabsContainerHandler handler = createHandler(0, false);
        sharedParameters.defaultTabFeaturesObjectStyle = handler.getTabFeaturesObjectStyle();
        Color defaultColour = sharedParameters.defaultTabFeaturesObjectStyle.getColor();

        // the watcher only reacts to disabledTextColor on old Burp, so these calls must not recurse
        handler.setColor(Color.RED, true);
        assertEquals(Color.RED, toolTabbedPane.getForegroundAt(0));

        handler.setToDefault(true);
        assertEquals(defaultColour, toolTabbedPane.getForegroundAt(0));
    }

    @Test
    void setToDefaultDoesNotChangeTheSharedDefaultStyle() {
        sharedParameters.isTabTextColorSetByBackground = false;
        SubTabsContainerHandler handler = createHandler(0, true);
        sharedParameters.defaultTabFeaturesObjectStyle = handler.getTabFeaturesObjectStyle();
        Color defaultColour = sharedParameters.defaultTabFeaturesObjectStyle.getColor();

        handler.setToDefault(true);

        // the shared default style must keep the real default colour
        assertEquals(defaultColour, sharedParameters.defaultTabFeaturesObjectStyle.getColor());
        assertTrue(handler.isDefaultColour(defaultColour));
    }

    @Test
    void setToDefaultWithoutResetMarkerAppliesTheRealDefaultColour() {
        sharedParameters.isTabTextColorSetByBackground = false;
        SubTabsContainerHandler handler = createHandler(0, true);
        sharedParameters.defaultTabFeaturesObjectStyle = handler.getTabFeaturesObjectStyle();
        Color defaultColour = sharedParameters.defaultTabFeaturesObjectStyle.getColor();

        handler.setColor(Color.RED, true);
        // this form is used at unload and at tab discovery, where no watcher is listening
        handler.setToDefault(true, false);

        assertEquals(defaultColour, toolTabbedPane.getForegroundAt(0));
    }

    @Test
    void setColorClearsAStaleTabBackgroundEvenWhenTheColourIsUnchanged() {
        sharedParameters.isTabTextColorSetByBackground = false;
        SubTabsContainerHandler handler = createHandler(0, true);
        handler.setColor(Color.RED, true);

        // simulate a leftover chip from the old colouring method while the colour already matches
        toolTabbedPane.setBackgroundAt(0, Color.MAGENTA);
        handler.setColor(Color.RED, true);

        assertEquals(Color.RED, toolTabbedPane.getForegroundAt(0));
        assertEquals(toolTabbedPane.getBackground(), toolTabbedPane.getBackgroundAt(0));
    }

    @Test
    void defaultStyleUsesTabbedPaneForegroundOnNewBurp() {
        Color tabbedPaneForeground = Color.decode("#123456");
        toolTabbedPane.setForeground(tabbedPaneForeground);
        sharedParameters.isTabTextColorSetByBackground = false;
        SubTabsContainerHandler handler = createHandler(0, true);

        // no preset default style, so setToDefault must build one from the tabbed pane foreground
        handler.setToDefault(true);

        assertNotNull(sharedParameters.defaultTabFeaturesObjectStyle);
        assertEquals(tabbedPaneForeground, sharedParameters.defaultTabFeaturesObjectStyle.getColor());
        // the real default colour is applied to the tab, so it reads back as the default
        assertEquals(tabbedPaneForeground, toolTabbedPane.getForegroundAt(0));
    }

    @Test
    void defaultStyleUsesTabbedPaneForegroundOnOldBurp() {
        Color tabbedPaneForeground = Color.decode("#123456");
        Color textFieldForeground = Color.decode("#654321");
        Object oldTabbedPaneValue = UIManager.put("TabbedPane.foreground", tabbedPaneForeground);
        Object oldTextFieldValue = UIManager.put("TextField.foreground", textFieldForeground);
        try {
            sharedParameters.isTabTextColorSetByBackground = true;
            SubTabsContainerHandler handler = createHandler(0, true);

            handler.setToDefault(true);

            assertNotNull(sharedParameters.defaultTabFeaturesObjectStyle);
            assertEquals(tabbedPaneForeground, sharedParameters.defaultTabFeaturesObjectStyle.getColor());
        } finally {
            UIManager.put("TabbedPane.foreground", oldTabbedPaneValue);
            UIManager.put("TextField.foreground", oldTextFieldValue);
        }
    }

    @Test
    void fontStyleChangesApplyToTheTabTitleTextField() {
        SubTabsContainerHandler handler = createHandler(0, true);

        handler.setBold(true, true);
        assertTrue(tabOneTitleField.getFont().isBold());
        assertTrue(handler.isBold());

        handler.setItalic(true, true);
        assertTrue(tabOneTitleField.getFont().isItalic());
        assertTrue(handler.isItalic());

        handler.setFontSize(18f, true);
        assertEquals(18f, handler.getFontSize());
        assertEquals(18, tabOneTitleField.getFont().getSize());

        handler.setBold(false, true);
        assertFalse(tabOneTitleField.getFont().isBold());
    }
}
