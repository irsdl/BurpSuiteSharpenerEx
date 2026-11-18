// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Discovery is scoped to the API-provided frame/tool, never an arbitrary visible window. */
public final class BurpUIDiscovery {
    private BurpUIDiscovery() {}
    private record Entry(Component component, int depth) {}
    private static final int MAX_DEPTH = 16;
    private static final int MAX_COMPONENTS = 10000;
    private static final Set<String> MAIN_TOOLS = Set.of("Dashboard", "Target", "Proxy", "Intruder",
            "Repeater", "Sequencer", "Decoder", "Comparer", "Logger", "Extensions", "Extender");

    public static <T> T onEdt(Supplier<T> operation) {
        if (SwingUtilities.isEventDispatchThread())
            return operation.get();
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(operation.get()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while accessing the Burp UI", e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException failure) throw failure;
            if (e.getCause() instanceof Error failure) throw failure;
            throw new IllegalStateException(e.getCause());
        }
        return result.get();
    }

    /** Require multiple distinct tool names; unknown/new tabs do not invalidate a match. */
    public static boolean isMainTabs(JTabbedPane pane) {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < pane.getTabCount(); i++) {
            String title = pane.getTitleAt(i);
            if (title == null) continue;
            for (String tool : MAIN_TOOLS)
                if (tool.equalsIgnoreCase(title.trim())) names.add(tool);
        }
        return names.size() >= 2;
    }

    public static JTabbedPane findMainTabs(Component root) {
        return findTabs(root, true);
    }

    /** Stop at the first tab layer; do not mistake request/response editors for tool tabs. */
    public static JTabbedPane findToolTabs(Component toolContent) {
        return findTabs(toolContent, false);
    }

    private static JTabbedPane findTabs(Component root, boolean main) {
        if (root == null) return null;
        ArrayDeque<Entry> queue = new ArrayDeque<>();
        queue.add(new Entry(root, 0));
        JTabbedPane match = null;
        int matchDepth = -1;
        int visited = 0;
        while (!queue.isEmpty()) {
            Entry entry = queue.removeFirst();
            // Breadth-first ordering lets us finish the candidate's entire layer
            // without walking deeper, unrelated sidebars. Burp 2026.11 has such
            // a sidebar: its depth previously discarded an already valid match.
            if (match != null && entry.depth() > matchDepth) return match;
            if (++visited > MAX_COMPONENTS) return null;
            if (entry.component() instanceof JTabbedPane tabs) {
                if (!main || isMainTabs(tabs)) {
                    // Ambiguous layouts are left untouched instead of selecting an unrelated pane.
                    if (match != null) return null;
                    match = tabs;
                    matchDepth = entry.depth();
                }
                // A tabbed pane marks a semantic boundary. Never search inside editor/tool tabs.
                continue;
            }
            if (match != null && entry.depth() == matchDepth) continue;
            if (entry.component() instanceof Container container) {
                if (entry.depth() >= MAX_DEPTH && container.getComponentCount() > 0) return null;
                for (Component child : container.getComponents())
                    queue.addLast(new Entry(child, entry.depth() + 1));
            }
        }
        return match;
    }

    public static Component toolContent(JTabbedPane mainTabs, BurpUITools.MainTabs tool) {
        if (mainTabs == null || tool == null) return null;
        for (int i = 0; i < mainTabs.getTabCount(); i++)
            if (tool.toString().equalsIgnoreCase(mainTabs.getTitleAt(i)))
                return mainTabs.getComponentAt(i);
        return null;
    }

    /** Walk ancestors rather than assuming one particular number of wrapper panels. */
    public static BurpUITools.MainTabs toolFor(Component component) {
        for (Component child = component; child != null; child = child.getParent()) {
            if (child.getParent() instanceof JTabbedPane parent) {
                int index = parent.indexOfComponent(child);
                if (index >= 0) {
                    BurpUITools.MainTabs tool = BurpUITools.getMainTabsObjFromString(parent.getTitleAt(index));
                    if (tool != BurpUITools.MainTabs.None) return tool;
                }
            }
        }
        Window window = SwingUtilities.getWindowAncestor(component);
        return window instanceof Frame frame
                ? BurpUITools.getMainTabsObjFromString(frame.getTitle().replaceFirst("^Burp ", ""))
                : BurpUITools.MainTabs.None;
    }
}
