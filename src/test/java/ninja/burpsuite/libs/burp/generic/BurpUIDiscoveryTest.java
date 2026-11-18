// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

import burp.api.montoya.MontoyaApi;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BurpUIDiscoveryTest {
    private JTabbedPane mainTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Proxy", new JPanel());
        tabs.addTab("Repeater", new JPanel());
        tabs.addTab("Academy", new JPanel()); // A new/unknown tool must not break discovery.
        return tabs;
    }

    private JPanel wrapped(Component component, int depth) {
        JPanel result = new JPanel();
        result.add(new JLabel("unrelated sibling"));
        if (depth == 0) result.add(component);
        else result.add(wrapped(component, depth - 1));
        return result;
    }

    @Test void findsDirectAndArbitrarilyWrappedMainTabsWithoutUsingChildIndices() {
        BurpUIDiscovery.onEdt(() -> {
            JTabbedPane tabs = mainTabs();
            assertSame(tabs, BurpUIDiscovery.findMainTabs(tabs));
            assertSame(tabs, BurpUIDiscovery.findMainTabs(wrapped(tabs, 8)));
            return null;
        });
    }

    @Test void rejectsEmptyUnrelatedAmbiguousAndExcessivelyDeepTrees() {
        BurpUIDiscovery.onEdt(() -> {
            assertNull(BurpUIDiscovery.findMainTabs(null));
            assertNull(BurpUIDiscovery.findMainTabs(new JPanel()));
            JTabbedPane editor = new JTabbedPane();
            editor.addTab("Raw", mainTabs()); // Do not descend into arbitrary editor panes.
            assertNull(BurpUIDiscovery.findMainTabs(editor));
            JPanel ambiguous = new JPanel();
            ambiguous.add(mainTabs()); ambiguous.add(mainTabs());
            assertNull(BurpUIDiscovery.findMainTabs(ambiguous));
            assertNull(BurpUIDiscovery.findMainTabs(wrapped(mainTabs(), 20)));
            return null;
        });
    }

    @Test void toolDiscoveryStopsBeforeEditorsAndHandlesEmptyAndWrappedTools() {
        BurpUIDiscovery.onEdt(() -> {
            JTabbedPane tool = new JTabbedPane();
            assertSame(tool, BurpUIDiscovery.findToolTabs(wrapped(tool, 5)));
            JTabbedPane editor = new JTabbedPane();
            editor.addTab("Raw", new JPanel());
            tool.addTab("1", editor);
            assertSame(tool, BurpUIDiscovery.findToolTabs(wrapped(tool, 3)));
            JPanel ambiguous = new JPanel(); ambiguous.add(tool); ambiguous.add(new JTabbedPane());
            assertNull(BurpUIDiscovery.findToolTabs(ambiguous));
            return null;
        });
    }

    @Test void mainTabsSurviveAnUnrelatedDeepSidebarAsInBurp202611() {
        BurpUIDiscovery.onEdt(() -> {
            JTabbedPane main = mainTabs();
            JPanel content = new JPanel();
            JPanel mainLayer = new JPanel(); mainLayer.add(main);
            JPanel sidebar = wrapped(new JPanel(), 24);
            // Test both sibling orders: discovery must not depend on component order.
            content.add(mainLayer); content.add(sidebar);
            assertSame(main, BurpUIDiscovery.findMainTabs(content));
            content.removeAll(); content.add(sidebar); content.add(mainLayer);
            assertSame(main, BurpUIDiscovery.findMainTabs(content));
            return null;
        });
    }

    @Test void nearestTabLayerWinsButEquallyNearMainPanesRemainAmbiguous() {
        BurpUIDiscovery.onEdt(() -> {
            JTabbedPane nearest = mainTabs();
            JPanel content = new JPanel();
            content.add(nearest); content.add(wrapped(mainTabs(), 4));
            assertSame(nearest, BurpUIDiscovery.findMainTabs(content));
            content.add(mainTabs());
            assertNull(BurpUIDiscovery.findMainTabs(content));
            return null;
        });
    }

    @Test void toolTabsSurviveAnUnrelatedDeepSidebar() {
        BurpUIDiscovery.onEdt(() -> {
            JTabbedPane tool = new JTabbedPane();
            tool.addTab("1", new JPanel());
            JPanel content = new JPanel();
            content.add(wrapped(new JPanel(), 24)); content.add(tool);
            assertSame(tool, BurpUIDiscovery.findToolTabs(content));
            return null;
        });
    }

    @Test void identifiesToolThroughMultipleNewWrappers() {
        BurpUIDiscovery.onEdt(() -> {
            JTabbedPane main = mainTabs();
            JTabbedPane tool = new JTabbedPane();
            main.setComponentAt(1, wrapped(tool, 6));
            assertEquals(BurpUITools.MainTabs.Repeater, BurpUIDiscovery.toolFor(tool));
            assertSame(tool, BurpUIDiscovery.findToolTabs(BurpUIDiscovery.toolContent(main, BurpUITools.MainTabs.Repeater)));
            assertEquals(BurpUITools.MainTabs.None, BurpUIDiscovery.toolFor(new JPanel()));
            return null;
        });
    }

    @Test void discardsReplacedMainPaneAndMenuBarInsteadOfUsingStaleCache() {
        BurpUIDiscovery.onEdt(() -> {
            MontoyaApi api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS);
            JFrame frame = mock(JFrame.class);
            when(api.userInterface().swingUtils().suiteFrame()).thenReturn(frame);
            BurpExtensionSharedParameters parameters = new BurpExtensionSharedParameters();
            parameters.montoyaApi = api;
            JTabbedPane oldTabs = mainTabs();
            JPanel oldContent = wrapped(oldTabs, 1);
            when(frame.getContentPane()).thenReturn(oldContent);
            assertSame(oldTabs, parameters.get_rootTabbedPaneUsingMontoya());
            JTabbedPane replacement = mainTabs();
            when(frame.getContentPane()).thenReturn(wrapped(replacement, 5));
            assertSame(replacement, parameters.get_rootTabbedPaneUsingMontoya());
            JMenuBar bar = new JMenuBar(); when(frame.getJMenuBar()).thenReturn(bar);
            assertSame(bar, parameters.get_mainMenuBarUsingMontoya());
            when(frame.getJMenuBar()).thenReturn(null);
            assertNull(parameters.get_mainMenuBarUsingMontoya());
            return null;
        });
    }

    @Test void executesOnEdtAndPropagatesFailuresToTheFeatureBoundary() {
        assertTrue(BurpUIDiscovery.onEdt(SwingUtilities::isEventDispatchThread));
        assertThrows(IllegalArgumentException.class, () -> BurpUIDiscovery.onEdt(() -> {
            throw new IllegalArgumentException("changed layout");
        }));
    }
}
