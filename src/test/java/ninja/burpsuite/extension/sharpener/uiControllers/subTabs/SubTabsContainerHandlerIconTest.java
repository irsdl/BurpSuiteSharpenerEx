// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObjectStyle;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.MouseEventForwarder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

// These tests mimic the Burp 2026 tab bar: the tab component is a BorderLayout container
// where the title text field fills the centre. The icon feature must not break that layout,
// because clicks outside the title area are swallowed by Burp and would stop selecting the tab.
public class SubTabsContainerHandlerIconTest {

    private ExtensionSharedParameters sharedParameters;
    private JTabbedPane toolTabbedPane;
    private JTextField tabOneTitleField;
    private JButton tabOneCloseButton;

    @BeforeEach
    void setUp() {
        sharedParameters = mock(ExtensionSharedParameters.class);
        sharedParameters.allSubTabContainerHandlers = new HashMap<>();
        sharedParameters.allSubTabContainerHandlers.put(BurpUITools.MainTabs.Repeater, new ArrayList<>());
        sharedParameters.isTabGroupSupportedByDefault = true;
        sharedParameters.isDarkMode = false;
        // needed by setIcon to load the icon image from the extension resources
        sharedParameters.extensionClass = SubTabsContainerHandler.class;

        JTabbedPane mainTabbedPane = new JTabbedPane();
        toolTabbedPane = new JTabbedPane();
        mainTabbedPane.addTab("Repeater", toolTabbedPane);
    }

    // mimics the Burp 2026 tab component: BorderLayout, title in the centre, close button at the end
    private JTextField addNewBurpSubTab(String title) {
        toolTabbedPane.addTab(title, new JPanel());
        JPanel tabComponent = new JPanel(new BorderLayout(10, 0));
        JTextField titleTextField = new JTextField(title);
        // Burp centres the tab title by default
        titleTextField.setHorizontalAlignment(SwingConstants.CENTER);
        tabComponent.add(titleTextField, BorderLayout.CENTER);
        tabOneCloseButton = new JButton("x");
        // the handler finds the close button by a name containing "close"
        tabOneCloseButton.setName("closeButton");
        // Burp shows the close button only on the selected tab, so it starts hidden here
        tabOneCloseButton.setVisible(false);
        tabComponent.add(tabOneCloseButton, BorderLayout.LINE_END);
        toolTabbedPane.setTabComponentAt(toolTabbedPane.getTabCount() - 1, tabComponent);
        return titleTextField;
    }

    // mimics the old Burp tab component after this extension replaced its layout
    private JTextField addDamagedFlowLayoutSubTab(String title) {
        toolTabbedPane.addTab(title, new JPanel());
        JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JTextField titleTextField = new JTextField(title);
        tabComponent.add(titleTextField);
        tabOneCloseButton = new JButton("x");
        tabComponent.add(tabOneCloseButton);
        toolTabbedPane.setTabComponentAt(toolTabbedPane.getTabCount() - 1, tabComponent);
        return titleTextField;
    }

    // stands in for Burp's own custom tab layout manager: a layout whose class is NOT in
    // java.awt / javax.swing, which is how the handler tells Burp's layout from a damaged one
    private static class FakeBurpTabLayout extends BorderLayout {
        private static final long serialVersionUID = 1L;

        FakeBurpTabLayout() {
            super(10, 0);
        }
    }

    // mimics a real Burp 2026 tab: a custom (burp.*-like) layout with the title in the centre
    private JTextField addBurpLikeSubTab(String title) {
        toolTabbedPane.addTab(title, new JPanel());
        JPanel tabComponent = new JPanel(new FakeBurpTabLayout());
        JTextField titleTextField = new JTextField(title);
        tabComponent.add(titleTextField, BorderLayout.CENTER);
        tabOneCloseButton = new JButton("x");
        tabOneCloseButton.setName("closeButton");
        tabOneCloseButton.setVisible(false);
        tabComponent.add(tabOneCloseButton, BorderLayout.LINE_END);
        toolTabbedPane.setTabComponentAt(toolTabbedPane.getTabCount() - 1, tabComponent);
        return titleTextField;
    }

    private SubTabsContainerHandler createHandler(int tabIndex) {
        return new SubTabsContainerHandler(sharedParameters, toolTabbedPane, tabIndex, true);
    }

    // a real (non-comparison) handler installs the tab watcher, which adds the whole-tab click forwarder
    private SubTabsContainerHandler createHandlerWithWatcher(int tabIndex) {
        return new SubTabsContainerHandler(sharedParameters, toolTabbedPane, tabIndex, false);
    }

    private boolean tabHasForwarder(JComponent tabComponent) {
        for (var listener : tabComponent.getMouseListeners()) {
            if (listener instanceof MouseEventForwarder)
                return true;
        }
        return false;
    }

    private JComponent tabComponent(int tabIndex) {
        return (JComponent) toolTabbedPane.getTabComponentAt(tabIndex);
    }

    @Test
    void newBurpSetIconPacksTheIconAndTitleTightly() {
        sharedParameters.isTabTextColorSetByBackground = false;
        tabOneTitleField = addNewBurpSubTab("1");
        SubTabsContainerHandler handler = createHandler(0);

        handler.setIcon("tick", 16, true);

        JComponent tabComponent = tabComponent(0);
        // a tight FlowLayout sizes the title to its text, so there is no gap or trailing space
        assertInstanceOf(FlowLayout.class, tabComponent.getLayout());
        assertInstanceOf(JLabel.class, tabComponent.getComponent(0));
        assertEquals("tick:16", tabComponent.getComponent(0).getName());
        // the icon comes first, then the title
        assertSame(tabOneTitleField, tabComponent.getComponent(1));
        assertEquals("tick", handler.getIconString());
        assertEquals(16, handler.getIconSize());
    }

    @Test
    void newBurpRealTabIsFullyClickableViaForwarder() {
        sharedParameters.isTabTextColorSetByBackground = false;
        tabOneTitleField = addNewBurpSubTab("1");
        // a real tab installs the watcher, which makes the whole tab clickable even without an icon.
        // Burp swallows clicks on the tab free space, so this forwarder is what lets middle-click
        // open the menu and a left click on the padding select the tab.
        createHandlerWithWatcher(0);

        JComponent tabComponent = tabComponent(0);
        assertTrue(tabHasForwarder(tabComponent));

        final MouseEvent[] receivedEvent = new MouseEvent[1];
        toolTabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                receivedEvent[0] = e;
            }
        });
        tabComponent.dispatchEvent(new MouseEvent(tabComponent, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 2, 2, 1, false));

        assertNotNull(receivedEvent[0]);
        assertSame(toolTabbedPane, receivedEvent[0].getComponent());
    }

    @Test
    void newBurpRemoveIconRestoresTheBorderLayoutAndKeepsTheForwarder() {
        sharedParameters.isTabTextColorSetByBackground = false;
        tabOneTitleField = addNewBurpSubTab("1");
        SubTabsContainerHandler handler = createHandlerWithWatcher(0);
        handler.setIcon("tick", 16, true);

        handler.removeIcon(true);

        JComponent tabComponent = tabComponent(0);
        // the normal Burp BorderLayout is restored with the title filling the centre again
        assertInstanceOf(BorderLayout.class, tabComponent.getLayout());
        assertSame(tabOneTitleField, tabComponent.getComponent(0));
        assertEquals(BorderLayout.CENTER, ((BorderLayout) tabComponent.getLayout()).getConstraints(tabOneTitleField));
        // the whole-tab forwarder stays so the tab is still fully clickable after the icon is removed
        assertTrue(tabHasForwarder(tabComponent));
    }

    @Test
    void newBurpRemoveIconRestoresBurpsOwnCapturedLayout() {
        sharedParameters.isTabTextColorSetByBackground = false;
        // a tab with Burp's own custom layout: the handler must capture it and restore it on reset,
        // so the tab keeps its native width instead of shrinking to a rebuilt BorderLayout
        tabOneTitleField = addBurpLikeSubTab("1");
        JComponent tabComponent = tabComponent(0);
        LayoutManager burpLayout = tabComponent.getLayout();
        assertInstanceOf(FakeBurpTabLayout.class, burpLayout);

        SubTabsContainerHandler handler = createHandlerWithWatcher(0);
        handler.setIcon("tick", 16, true);
        // adding an icon replaces Burp's layout
        assertNotSame(burpLayout, tabComponent.getLayout());

        handler.removeIcon(true);
        // removing the icon restores Burp's own captured layout instance, not a generic BorderLayout
        assertSame(burpLayout, tabComponent.getLayout());
    }

    @Test
    void newBurpSetToDefaultDoesNotForceTheCloseButtonVisible() {
        sharedParameters.isTabTextColorSetByBackground = false;
        tabOneTitleField = addNewBurpSubTab("1");
        SubTabsContainerHandler handler = createHandler(0);
        sharedParameters.defaultTabFeaturesObjectStyle = handler.getTabFeaturesObjectStyle();
        // Burp keeps the close button hidden on a non-selected tab
        assertFalse(tabOneCloseButton.isVisible());

        handler.setToDefault(true, false);

        // the extension must not turn the close button on for every tab
        assertFalse(tabOneCloseButton.isVisible());
    }

    @Test
    void newBurpSetToDefaultKeepsAVisibleCloseButtonVisible() {
        sharedParameters.isTabTextColorSetByBackground = false;
        tabOneTitleField = addNewBurpSubTab("1");
        tabOneCloseButton.setVisible(true);
        SubTabsContainerHandler handler = createHandler(0);
        sharedParameters.defaultTabFeaturesObjectStyle = handler.getTabFeaturesObjectStyle();

        handler.setToDefault(true, false);

        // a close button Burp already shows on the selected tab must stay visible
        assertTrue(tabOneCloseButton.isVisible());
    }

    @Test
    void newBurpAppliedStyleDoesNotForceTheCloseButtonVisible() {
        // reproduces the screenshot: a saved style on a non-selected tab must not show the close button
        sharedParameters.isTabTextColorSetByBackground = false;
        tabOneTitleField = addNewBurpSubTab("1");
        SubTabsContainerHandler handler = createHandler(0);
        assertFalse(tabOneCloseButton.isVisible());

        // a saved style with the close button flagged visible, as captured on an older build
        TabFeaturesObjectStyle savedStyle = new TabFeaturesObjectStyle("saved", "Dialog", 12, false, false, true, Color.RED, "", 0);
        handler.updateByTabFeaturesObjectStyle(savedStyle, true);

        // new Burp keeps managing the close button, so it stays hidden on the non-selected tab
        assertFalse(tabOneCloseButton.isVisible());
    }

    @Test
    void oldBurpAppliedStyleStillControlsTheCloseButton() {
        sharedParameters.isTabTextColorSetByBackground = true;
        tabOneTitleField = addNewBurpSubTab("1");
        SubTabsContainerHandler handler = createHandler(0);
        assertFalse(tabOneCloseButton.isVisible());

        TabFeaturesObjectStyle savedStyle = new TabFeaturesObjectStyle("saved", "Dialog", 12, false, false, true, Color.RED, "", 0);
        handler.updateByTabFeaturesObjectStyle(savedStyle, true);

        // old Burp did not manage the close button, so the extension still applies the saved state
        assertTrue(tabOneCloseButton.isVisible());
    }

    @Test
    void newBurpIconForwardsMouseEventsToTheTabbedPane() {
        sharedParameters.isTabTextColorSetByBackground = false;
        tabOneTitleField = addNewBurpSubTab("1");
        SubTabsContainerHandler handler = createHandler(0);
        handler.setIcon("tick", 16, true);

        JLabel iconLabel = (JLabel) tabComponent(0).getComponent(0);
        boolean hasForwarder = false;
        for (var listener : iconLabel.getMouseListeners()) {
            if (listener instanceof MouseEventForwarder)
                hasForwarder = true;
        }
        assertTrue(hasForwarder);

        // a press on the icon must arrive at the tabbed pane, which is what selects the tab
        final MouseEvent[] receivedEvent = new MouseEvent[1];
        toolTabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                receivedEvent[0] = e;
            }
        });
        iconLabel.dispatchEvent(new MouseEvent(iconLabel, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 2, 2, 1, false));

        assertNotNull(receivedEvent[0]);
        assertSame(toolTabbedPane, receivedEvent[0].getComponent());
    }

    @Test
    void newBurpRemoveIconKeepsTheBorderLayout() {
        sharedParameters.isTabTextColorSetByBackground = false;
        tabOneTitleField = addNewBurpSubTab("1");
        SubTabsContainerHandler handler = createHandler(0);
        handler.setIcon("tick", 16, true);

        handler.removeIcon(true);

        JComponent tabComponent = tabComponent(0);
        assertInstanceOf(JTextField.class, tabComponent.getComponent(0));
        assertInstanceOf(BorderLayout.class, tabComponent.getLayout());
        assertEquals(BorderLayout.CENTER, ((BorderLayout) tabComponent.getLayout()).getConstraints(tabOneTitleField));
    }

    @Test
    void removeIconRepairsATabDamagedByAnOlderVersion() {
        sharedParameters.isTabTextColorSetByBackground = false;
        tabOneTitleField = addDamagedFlowLayoutSubTab("1");
        SubTabsContainerHandler handler = createHandler(0);

        // no icon is present; removeIcon is called for every tab on reset and unload
        handler.removeIcon(true);

        JComponent tabComponent = tabComponent(0);
        assertInstanceOf(BorderLayout.class, tabComponent.getLayout());
        BorderLayout layout = (BorderLayout) tabComponent.getLayout();
        assertEquals(BorderLayout.CENTER, layout.getConstraints(tabOneTitleField));
        assertEquals(BorderLayout.LINE_END, layout.getConstraints(tabOneCloseButton));
    }

    @Test
    void oldBurpSetIconStillUsesTheFlowLayout() {
        sharedParameters.isTabTextColorSetByBackground = true;
        tabOneTitleField = addNewBurpSubTab("1");
        SubTabsContainerHandler handler = createHandler(0);

        handler.setIcon("tick", 16, true);

        JComponent tabComponent = tabComponent(0);
        assertInstanceOf(FlowLayout.class, tabComponent.getLayout());
        assertInstanceOf(JLabel.class, tabComponent.getComponent(0));

        // old Burp must not be touched by the layout repair
        handler.removeIcon(true);
        assertInstanceOf(FlowLayout.class, tabComponent.getLayout());
    }
}
