// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.libs.generic.uiObjFinder;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class UIWalkerTest {

    @Test
    void getCurrentJComponentReturnsNullForNullInput() {
        assertNull(UIWalker.getCurrentJComponent(null));
    }

    @Test
    void getCurrentJComponentReturnsTheSameJComponent() {
        JPanel panel = new JPanel();
        assertSame(panel, UIWalker.getCurrentJComponent(panel));
    }

    @Test
    void getCurrentJComponentConvertsAwtContainerToItsChild() {
        Panel awtPanel = new Panel(null);
        awtPanel.setSize(100, 100);
        JPanel child = new JPanel();
        awtPanel.add(child);
        child.setBounds(0, 0, 50, 50);

        assertSame(child, UIWalker.getCurrentJComponent(awtPanel));
    }

    @Test
    void subComponentSearchReturnsTheRootWhenItMatches() {
        JTextField textField = new JTextField();
        Component found = UIWalker.findUIObjectInSubComponents(textField, 0, new UiSpecObject(JTextField.class));
        assertSame(textField, found);
    }

    @Test
    void subComponentSearchFindsNestedComponent() {
        JPanel root = new JPanel();
        JPanel middle = new JPanel();
        JTextField target = new JTextField();
        root.add(middle);
        middle.add(target);

        Component found = UIWalker.findUIObjectInSubComponents(root, 1, new UiSpecObject(JTextField.class));
        assertSame(target, found);
    }

    @Test
    void subComponentSearchStopsAtMaxDepth() {
        JPanel root = new JPanel();
        JPanel level1 = new JPanel();
        JPanel level2 = new JPanel();
        JTextField target = new JTextField();
        root.add(level1);
        level1.add(level2);
        level2.add(target);

        // the target is three levels down, so it needs a depth of at least 2
        assertNull(UIWalker.findUIObjectInSubComponents(root, 1, new UiSpecObject(JTextField.class)));
        assertSame(target, UIWalker.findUIObjectInSubComponents(root, 2, new UiSpecObject(JTextField.class)));
    }

    @Test
    void subComponentSearchWithNullRootReturnsNull() {
        // this happens when the result of a previous search is used as the new root
        assertNull(UIWalker.findUIObjectInSubComponents(null, 3, new UiSpecObject(JTabbedPane.class)));
    }

    @Test
    void subComponentSearchSkipsExcludedComponents() {
        JPanel root = new JPanel();
        JTextField first = new JTextField();
        JTextField second = new JTextField();
        root.add(first);
        root.add(second);

        UiSpecObject spec = new UiSpecObject(JTextField.class);
        assertSame(first, UIWalker.findUIObjectInSubComponents(root, 0, spec));
        assertSame(second, UIWalker.findUIObjectInSubComponentsWithExclusions(root, 0, spec, new Component[]{first}));
        assertNull(UIWalker.findUIObjectInSubComponentsWithExclusions(root, 0, spec, new Component[]{first, second}));
    }

    @Test
    void nestedTabbedPaneLookupWorksLikeDetachedToolSearch() {
        // the same shape as the detached Repeater window lookup
        JTabbedPane outerTabbedPane = new JTabbedPane();
        JTabbedPane innerTabbedPane = new JTabbedPane();
        JPanel wrapper = new JPanel();
        wrapper.add(innerTabbedPane);
        outerTabbedPane.add("tool", wrapper);

        UiSpecObject spec = new UiSpecObject(JTabbedPane.class);
        Component firstFound = UIWalker.findUIObjectInSubComponents(outerTabbedPane, 6, spec);
        assertSame(outerTabbedPane, firstFound);

        Component secondFound = UIWalker.findUIObjectInSubComponentsWithExclusions(firstFound, 2, spec, new Component[]{firstFound});
        assertSame(innerTabbedPane, secondFound);
    }

    @Test
    void parentSearchReturnsTheRootWhenItMatches() {
        JTextField textField = new JTextField();
        Component found = UIWalker.findUIObjectInParentComponents(textField, 0, new UiSpecObject(JTextField.class));
        assertSame(textField, found);
    }

    @Test
    void parentSearchFindsAncestorWithinMaxDepth() {
        JPanel outer = new JPanel();
        outer.setName("outer");
        JPanel inner = new JPanel();
        JTextField child = new JTextField();
        outer.add(inner);
        inner.add(child);

        UiSpecObject spec = new UiSpecObject(JPanel.class);
        spec.set_name("outer");

        // the named ancestor is two levels up, so it needs a depth of at least 1
        assertNull(UIWalker.findUIObjectInParentComponents(child, 0, spec));
        assertSame(outer, UIWalker.findUIObjectInParentComponents(child, 1, spec));
    }

    @Test
    void parentSearchSkipsExcludedComponents() {
        JPanel outer = new JPanel();
        JPanel inner = new JPanel();
        JTextField child = new JTextField();
        outer.add(inner);
        inner.add(child);

        UiSpecObject spec = new UiSpecObject(JPanel.class);
        assertSame(inner, UIWalker.findUIObjectInParentComponents(child, 1, spec));
        assertSame(outer, UIWalker.findUIObjectInParentComponentsWithExclusions(child, 1, spec, new Component[]{inner}));
    }

    @Test
    void neighbourSearchFindsSibling() {
        JPanel parent = new JPanel();
        JPanel root = new JPanel();
        JTextField sibling = new JTextField();
        parent.add(root);
        parent.add(sibling);

        Component found = UIWalker.findUIObjectInNeighbourComponents(root, new UiSpecObject(JTextField.class));
        assertSame(sibling, found);
    }

    @Test
    void neighbourSearchDoesNotReturnTheParent() {
        JPanel parent = new JPanel();
        parent.setName("parentPanel");
        JPanel root = new JPanel();
        parent.add(root);

        UiSpecObject spec = new UiSpecObject(JPanel.class);
        spec.set_name("parentPanel");
        assertNull(UIWalker.findUIObjectInNeighbourComponents(root, spec));
    }

    @Test
    void componentsArraySearchFindsFirstMatch() {
        JPanel panel = new JPanel();
        JTextField first = new JTextField();
        JTextField second = new JTextField();
        Component[] components = {panel, first, second};

        UiSpecObject spec = new UiSpecObject(JTextField.class);
        assertSame(first, UIWalker.findUIObjectInComponents(components, spec));
        assertSame(second, UIWalker.findUIObjectInComponentsWithExclusions(components, spec, new Component[]{first}));
    }

    @Test
    void componentsArraySearchHandlesNullArray() {
        assertNull(UIWalker.findUIObjectInComponents(null, new UiSpecObject(JTextField.class)));
    }

    @Test
    void containsComponentChecks() {
        JPanel panel = new JPanel();
        JPanel other = new JPanel();

        assertTrue(UIWalker.containsComponent(new Component[]{panel}, panel));
        assertFalse(UIWalker.containsComponent(new Component[]{other}, panel));
        assertFalse(UIWalker.containsComponent(null, panel));
        assertFalse(UIWalker.containsComponent(new Component[]{panel}, null));
    }
}
