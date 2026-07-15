// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.generic.uiObjFinder;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UiSpecObjectTest {

    @Test
    void nullComponentIsNotCompatible() {
        UiSpecObject spec = new UiSpecObject(JComponent.class);
        assertFalse(spec.isCompatible(null));
    }

    @Test
    void emptySpecMatchesAnyComponent() {
        UiSpecObject spec = new UiSpecObject();
        assertTrue(spec.isCompatible(new JPanel()));
        assertTrue(spec.isCompatible(new Panel()));
    }

    @Test
    void objectTypeMatchesSameClassAndSubclass() {
        UiSpecObject spec = new UiSpecObject(JComponent.class);
        assertTrue(spec.isCompatible(new JTextField()));

        UiSpecObject strictSpec = new UiSpecObject(JTextField.class);
        assertTrue(strictSpec.isCompatible(new JTextField()));
        assertFalse(strictSpec.isCompatible(new JPanel()));
    }

    @Test
    void isJComponentRejectsPlainAwtComponent() {
        UiSpecObject spec = new UiSpecObject();
        spec.set_isJComponent(true);
        assertFalse(spec.isCompatible(new Panel()));
        assertTrue(spec.isCompatible(new JPanel()));
    }

    @Test
    void exactNameMatch() {
        JPanel panel = new JPanel();
        panel.setName("myPanel");

        UiSpecObject spec = new UiSpecObject();
        spec.set_name("myPanel");
        assertTrue(spec.isCompatible(panel));

        spec.set_name("myPanelX");
        assertFalse(spec.isCompatible(panel));
    }

    @Test
    void partialNameMatch() {
        JPanel panel = new JPanel();
        panel.setName("closeButton99");

        UiSpecObject spec = new UiSpecObject();
        spec.set_isPartialName(true);
        spec.set_name("close");
        assertTrue(spec.isCompatible(panel));

        spec.set_name("open");
        assertFalse(spec.isCompatible(panel));
    }

    @Test
    void caseInsensitiveNameMatchesWhenExpectedNameHasUpperCase() {
        JPanel panel = new JPanel();
        panel.setName("closeButton");

        UiSpecObject spec = new UiSpecObject();
        spec.set_isPartialName(true);
        spec.set_isCaseSensitiveName(false);
        spec.set_name("CLOSE");
        assertTrue(spec.isCompatible(panel));
    }

    @Test
    void caseInsensitiveExactNameMatch() {
        JPanel panel = new JPanel();
        panel.setName("Repeater");

        UiSpecObject spec = new UiSpecObject();
        spec.set_isCaseSensitiveName(false);
        spec.set_name("REPEATER");
        assertTrue(spec.isCompatible(panel));
    }

    @Test
    void caseSensitiveNameRejectsDifferentCase() {
        JPanel panel = new JPanel();
        panel.setName("Repeater");

        UiSpecObject spec = new UiSpecObject();
        spec.set_name("repeater");
        assertFalse(spec.isCompatible(panel));
    }

    @Test
    void componentWithNullNameIsRejectedWhenNameIsExpected() {
        UiSpecObject spec = new UiSpecObject();
        spec.set_name("anything");
        assertFalse(spec.isCompatible(new JPanel()));
    }

    @Test
    void widthAndHeightBoundaries() {
        JPanel panel = new JPanel();
        panel.setSize(100, 50);

        UiSpecObject spec = new UiSpecObject();
        spec.set_minWidth(100);
        spec.set_maxWidth(100);
        spec.set_minHeight(50);
        spec.set_maxHeight(50);
        assertTrue(spec.isCompatible(panel));

        UiSpecObject tooWideSpec = new UiSpecObject();
        tooWideSpec.set_minWidth(101);
        assertFalse(tooWideSpec.isCompatible(panel));

        UiSpecObject tooNarrowSpec = new UiSpecObject();
        tooNarrowSpec.set_maxWidth(99);
        assertFalse(tooNarrowSpec.isCompatible(panel));

        UiSpecObject tooTallSpec = new UiSpecObject();
        tooTallSpec.set_minHeight(51);
        assertFalse(tooTallSpec.isCompatible(panel));

        UiSpecObject tooShortSpec = new UiSpecObject();
        tooShortSpec.set_maxHeight(49);
        assertFalse(tooShortSpec.isCompatible(panel));
    }

    @Test
    void backgroundColorMatch() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.RED);

        UiSpecObject spec = new UiSpecObject();
        spec.set_backgroundColor(Color.RED);
        assertTrue(spec.isCompatible(panel));

        spec.set_backgroundColor(Color.BLUE);
        assertFalse(spec.isCompatible(panel));
    }

    @Test
    void nullBackgroundDoesNotThrow() {
        JPanel panel = new JPanel();
        panel.setBackground(null); // no parent, so getBackground() returns null

        UiSpecObject spec = new UiSpecObject();
        spec.set_backgroundColor(Color.RED);
        assertFalse(spec.isCompatible(panel));
    }

    @Test
    void componentCountBoundaries() {
        JPanel panel = new JPanel();
        panel.add(new JLabel());
        panel.add(new JLabel());

        UiSpecObject spec = new UiSpecObject();
        spec.set_minJComponentCount(2);
        spec.set_maxJComponentCount(2);
        assertTrue(spec.isCompatible(panel));

        UiSpecObject tooManySpec = new UiSpecObject();
        tooManySpec.set_minJComponentCount(3);
        assertFalse(tooManySpec.isCompatible(panel));

        UiSpecObject tooFewSpec = new UiSpecObject();
        tooFewSpec.set_maxJComponentCount(1);
        assertFalse(tooFewSpec.isCompatible(panel));
    }

    @Test
    void componentCountOnAwtComponentDoesNotThrow() {
        Panel awtPanel = new Panel();
        awtPanel.add(new JLabel());

        UiSpecObject spec = new UiSpecObject();
        spec.set_minJComponentCount(1);
        // a component count spec implies a JComponent, so a plain AWT container is rejected
        assertFalse(spec.isCompatible(awtPanel));
    }

    @Test
    void toolTipTextMatch() {
        JPanel panel = new JPanel();
        panel.setToolTipText("Reattach all tabs");

        UiSpecObject spec = new UiSpecObject();
        spec.set_toolTipText("Reattach all tabs");
        assertTrue(spec.isCompatible(panel));

        spec.set_toolTipText("Something else");
        assertFalse(spec.isCompatible(panel));
    }

    @Test
    void toolTipSpecOnAwtComponentDoesNotThrow() {
        UiSpecObject spec = new UiSpecObject();
        spec.set_toolTipText("anything");
        assertFalse(spec.isCompatible(new Panel()));

        UiSpecObject hasToolTipSpec = new UiSpecObject();
        hasToolTipSpec.set_hasToolTipText(true);
        assertFalse(hasToolTipSpec.isCompatible(new Panel()));
    }

    @Test
    void hasToolTipText() {
        JPanel withToolTip = new JPanel();
        withToolTip.setToolTipText("hello");
        JPanel withoutToolTip = new JPanel();

        UiSpecObject spec = new UiSpecObject();
        spec.set_hasToolTipText(true);
        assertTrue(spec.isCompatible(withToolTip));
        assertFalse(spec.isCompatible(withoutToolTip));

        spec.set_hasToolTipText(false);
        assertFalse(spec.isCompatible(withToolTip));
        assertTrue(spec.isCompatible(withoutToolTip));
    }

    @Test
    void hasMouseListenerWorksOnAnyComponent() {
        Panel awtPanel = new Panel();
        awtPanel.addMouseListener(new MouseAdapter() {
        });
        JPanel plainPanel = new JPanel();

        UiSpecObject spec = new UiSpecObject();
        spec.set_hasMouseListener(true);
        assertTrue(spec.isCompatible(awtPanel));
        assertFalse(spec.isCompatible(plainPanel));

        spec.set_hasMouseListener(false);
        assertFalse(spec.isCompatible(awtPanel));
        assertTrue(spec.isCompatible(plainPanel));
    }

    @Test
    void parentObjectTypeMatch() {
        JPanel parent = new JPanel();
        JTextField child = new JTextField();
        parent.add(child);

        UiSpecObject spec = new UiSpecObject();
        spec.set_parentObjectType(JPanel.class);
        assertTrue(spec.isCompatible(child));

        spec.set_parentObjectType(JScrollPane.class);
        assertFalse(spec.isCompatible(child));
    }

    @Test
    void parentObjectTypeWithNoParentDoesNotThrow() {
        UiSpecObject spec = new UiSpecObject();
        spec.set_parentObjectType(JPanel.class);
        assertFalse(spec.isCompatible(new JTextField()));
    }

    @Test
    void isShowingCheck() {
        // a component that is not in a displayed window is not showing
        JPanel panel = new JPanel();

        UiSpecObject notShowingSpec = new UiSpecObject();
        notShowingSpec.set_isShowing(false);
        assertTrue(notShowingSpec.isCompatible(panel));

        UiSpecObject showingSpec = new UiSpecObject();
        showingSpec.set_isShowing(true);
        assertFalse(showingSpec.isCompatible(panel));
    }

    @Test
    void frameTitleMatch() {
        JFrame frame = mock(JFrame.class);
        when(frame.getTitle()).thenReturn("Burp Suite Professional");

        UiSpecObject spec = new UiSpecObject();
        spec.set_frameTitle("Burp Suite Professional");
        assertTrue(spec.isCompatible(frame));

        spec.set_frameTitle("Another Title");
        assertFalse(spec.isCompatible(frame));
    }

    @Test
    void frameTitleRejectsNonFrameComponent() {
        UiSpecObject spec = new UiSpecObject();
        spec.set_frameTitle("Burp Suite Professional");
        assertFalse(spec.isCompatible(new JPanel()));
    }

    @Test
    void isCompatibleDoesNotChangeTheSpecObject() {
        UiSpecObject spec = new UiSpecObject();
        spec.set_minJComponentCount(1);
        spec.set_toolTipText("tip");

        spec.isCompatible(new JPanel());

        // the checks above imply a JComponent, but the stored flags must stay untouched
        assertFalse(spec.get_isJComponent());
        assertNull(spec.get_hasToolTipText());
    }

    @Test
    void combinedCriteriaMatchRealisticTabSpec() {
        // the same shape as the sub tab close button lookup
        JPanel closeButton = new JPanel();
        closeButton.setName("CloseTabButton");

        UiSpecObject spec = new UiSpecObject(JComponent.class);
        spec.set_isPartialName(true);
        spec.set_isCaseSensitiveName(false);
        spec.set_name("close");
        assertTrue(spec.isCompatible(closeButton));
    }
}
