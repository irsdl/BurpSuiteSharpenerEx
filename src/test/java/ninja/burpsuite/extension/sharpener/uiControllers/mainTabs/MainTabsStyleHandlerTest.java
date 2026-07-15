// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.uiControllers.mainTabs;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.burp.generic.ExtendedPreferences;
import ninja.burpsuite.libs.generic.MouseEventForwarder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// These tests mimic the Burp main tool tab bar: each tab has a custom tab component
// (a container with Burp's own layout holding the title text field).
public class MainTabsStyleHandlerTest {

    private ExtensionSharedParameters sharedParameters;
    private JTabbedPane rootTabbedPane;

    // stands in for Burp's own tab layout manager, so the restore can be checked by instance
    private static class FakeBurpTabLayout extends BorderLayout {
        private static final long serialVersionUID = 1L;
    }

    @BeforeEach
    void setUp() {
        sharedParameters = mock(ExtensionSharedParameters.class);
        sharedParameters.preferences = mock(ExtendedPreferences.class);
        // needed to load the theme icon images from the extension resources
        sharedParameters.extensionClass = MainTabsStyleHandler.class;
        // new Burp supports scrollable tabs by default, so unsetAll must not touch the layout policy
        sharedParameters.isSubTabScrollSupportedByDefault = true;

        when(sharedParameters.preferences.safeGetStringSetting("ToolsThemeName")).thenReturn("@irsdl");
        when(sharedParameters.preferences.safeGetStringSetting("ToolsThemeCustomPath")).thenReturn("");
        when(sharedParameters.preferences.safeGetSetting("ToolsIconSize", "16")).thenReturn("16");

        rootTabbedPane = new JTabbedPane();
        when(sharedParameters.get_rootTabbedPaneUsingMontoya()).thenReturn(rootTabbedPane);
    }

    private JTextField addBurpMainTab(String title) {
        rootTabbedPane.addTab(title, new JPanel());
        JPanel tabComponent = new JPanel(new FakeBurpTabLayout());
        JTextField titleTextField = new JTextField(title);
        tabComponent.add(titleTextField, BorderLayout.CENTER);
        rootTabbedPane.setTabComponentAt(rootTabbedPane.getTabCount() - 1, tabComponent);
        return titleTextField;
    }

    private JComponent tabComponent(int tabIndex) {
        return (JComponent) rootTabbedPane.getTabComponentAt(tabIndex);
    }

    private void flushUiThread() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    private void setStyle(BurpUITools.MainTabs tool) throws Exception {
        MainTabsStyleHandler.setMainTabsStyle_noUiLock(sharedParameters, tool);
        flushUiThread();
    }

    private boolean tabHasForwarder(JComponent component) {
        for (var listener : component.getMouseListeners()) {
            if (listener instanceof MouseEventForwarder)
                return true;
        }
        return false;
    }

    @Test
    void setStyleAddsTheThemeIconWithATightLayout() throws Exception {
        JTextField titleTextField = addBurpMainTab("Proxy");
        setStyle(BurpUITools.MainTabs.Proxy);

        JComponent tabComponent = tabComponent(0);
        // the icon comes first, then the original title text field
        JLabel iconLabel = assertInstanceOf(JLabel.class, tabComponent.getComponent(0));
        assertSame(titleTextField, tabComponent.getComponent(1));
        assertNotNull(iconLabel.getIcon());
        assertEquals(16, iconLabel.getIcon().getIconWidth());
        assertTrue(titleTextField.getFont().isBold());

        // a tight layout: a small gap between the icon and the title, no extra tab height
        FlowLayout layout = assertInstanceOf(FlowLayout.class, tabComponent.getLayout());
        assertEquals(4, layout.getHgap());
        assertEquals(0, layout.getVgap());
    }

    @Test
    void setStyleTwiceDoesNotAddASecondIcon() throws Exception {
        addBurpMainTab("Proxy");
        setStyle(BurpUITools.MainTabs.Proxy);
        setStyle(BurpUITools.MainTabs.Proxy);

        JComponent tabComponent = tabComponent(0);
        assertEquals(2, tabComponent.getComponentCount());
        assertInstanceOf(JLabel.class, tabComponent.getComponent(0));
        assertInstanceOf(JTextField.class, tabComponent.getComponent(1));
    }

    @Test
    void clickOnTheIconReachesTheTabbedPaneSoTheTabCanBeSelected() throws Exception {
        addBurpMainTab("Proxy");
        setStyle(BurpUITools.MainTabs.Proxy);

        JLabel iconLabel = (JLabel) tabComponent(0).getComponent(0);
        boolean hasForwarder = false;
        for (var listener : iconLabel.getMouseListeners()) {
            if (listener instanceof MouseEventForwarder)
                hasForwarder = true;
        }
        assertTrue(hasForwarder);

        // a press on the icon must arrive at the tabbed pane, which is what selects the tab
        final MouseEvent[] receivedEvent = new MouseEvent[1];
        rootTabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                receivedEvent[0] = e;
            }
        });
        iconLabel.dispatchEvent(new MouseEvent(iconLabel, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 2, 2, 1, false));

        assertNotNull(receivedEvent[0]);
        assertSame(rootTabbedPane, receivedEvent[0].getComponent());
    }

    @Test
    void clickOnTheTabFreeSpaceReachesTheTabbedPane() throws Exception {
        // the free space is the area above and below the title, or next to the icon,
        // which belongs to the tab component itself and would otherwise swallow the click
        addBurpMainTab("Proxy");
        setStyle(BurpUITools.MainTabs.Proxy);

        JComponent tabComponent = tabComponent(0);
        assertTrue(tabHasForwarder(tabComponent));

        final MouseEvent[] receivedEvent = new MouseEvent[1];
        rootTabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                receivedEvent[0] = e;
            }
        });
        tabComponent.dispatchEvent(new MouseEvent(tabComponent, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 2, 2, 1, false));

        assertNotNull(receivedEvent[0]);
        assertSame(rootTabbedPane, receivedEvent[0].getComponent());
    }

    @Test
    void unsetRestoresBurpsOwnLayoutFontAndBorder() throws Exception {
        JTextField titleTextField = addBurpMainTab("Proxy");
        JComponent tabComponent = tabComponent(0);
        LayoutManager burpLayout = tabComponent.getLayout();
        Font burpFont = titleTextField.getFont();
        Border burpBorder = titleTextField.getBorder();
        boolean burpOpaque = titleTextField.isOpaque();

        setStyle(BurpUITools.MainTabs.Proxy);
        assertNotSame(burpLayout, tabComponent.getLayout());

        MainTabsStyleHandler.unsetMainTabsStyle_noUiLock(sharedParameters, BurpUITools.MainTabs.Proxy);
        flushUiThread();

        // the icon is gone and Burp's own captured look is back, instance for instance
        assertSame(titleTextField, tabComponent.getComponent(0));
        assertEquals(1, tabComponent.getComponentCount());
        assertSame(burpLayout, tabComponent.getLayout());
        assertSame(burpFont, titleTextField.getFont());
        assertSame(burpBorder, titleTextField.getBorder());
        assertEquals(burpOpaque, titleTextField.isOpaque());

        // nothing of the extension is left behind on Burp's components
        assertFalse(tabHasForwarder(tabComponent));
        assertNull(tabComponent.getClientProperty(MainTabsStyleHandler.ORIGINAL_LAYOUT_KEY));
        assertNull(titleTextField.getClientProperty(MainTabsStyleHandler.ORIGINAL_FONT_KEY));
        assertNull(titleTextField.getClientProperty(MainTabsStyleHandler.ORIGINAL_BORDER_KEY));
        assertNull(titleTextField.getClientProperty(MainTabsStyleHandler.ORIGINAL_OPAQUE_KEY));
    }

    @Test
    void unsetAllRestoresEveryStyledTabEvenWhenCalledOffTheUiThread() throws Exception {
        JTextField proxyTitle = addBurpMainTab("Proxy");
        JTextField repeaterTitle = addBurpMainTab("Repeater");
        LayoutManager proxyLayout = tabComponent(0).getLayout();
        LayoutManager repeaterLayout = tabComponent(1).getLayout();

        setStyle(BurpUITools.MainTabs.Proxy);
        setStyle(BurpUITools.MainTabs.Repeater);

        // the test thread is not the UI thread, matching an unload started by Burp
        assertFalse(SwingUtilities.isEventDispatchThread());
        MainTabsStyleHandler.unsetAllMainTabsStyles(sharedParameters);

        assertSame(proxyTitle, tabComponent(0).getComponent(0));
        assertSame(proxyLayout, tabComponent(0).getLayout());
        assertFalse(proxyTitle.getFont().isBold());
        assertSame(repeaterTitle, tabComponent(1).getComponent(0));
        assertSame(repeaterLayout, tabComponent(1).getLayout());
        assertFalse(repeaterTitle.getFont().isBold());
    }

    @Test
    void unsetRepairsATabStyledBeforeTheOriginalLookWasSaved() throws Exception {
        // mimics a tab left styled by an older extension version: icon present, bold title,
        // and no saved original look
        rootTabbedPane.addTab("Proxy", new JPanel());
        JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel oldIconLabel = new JLabel();
        JTextField titleTextField = new JTextField("Proxy");
        titleTextField.setFont(titleTextField.getFont().deriveFont(Font.BOLD));
        tabComponent.add(oldIconLabel);
        tabComponent.add(titleTextField);
        rootTabbedPane.setTabComponentAt(0, tabComponent);

        MainTabsStyleHandler.unsetMainTabsStyle_noUiLock(sharedParameters, BurpUITools.MainTabs.Proxy);
        flushUiThread();

        assertSame(titleTextField, tabComponent.getComponent(0));
        assertFalse(titleTextField.getFont().isBold());
    }
}
