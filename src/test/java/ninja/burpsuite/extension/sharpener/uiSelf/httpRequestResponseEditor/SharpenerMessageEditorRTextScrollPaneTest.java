// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

// Tests for the message editor changes adopted from the early-adopter branch:
// undo/redo bindings, no placeholder text, and the Ctrl+Click quick copy experiment.
public class SharpenerMessageEditorRTextScrollPaneTest {

    private SharpenerMessageEditor_RTextScrollPane scrollPane;
    private RSyntaxTextArea textArea;

    @BeforeEach
    void setUp() {
        ExtensionSharedParameters sharedParameters = mock(ExtensionSharedParameters.class);
        scrollPane = new SharpenerMessageEditor_RTextScrollPane(sharedParameters);
        textArea = scrollPane.getTextArea();
    }

    @Test
    void editorStartsEmptyEditableAndWrapping() {
        assertEquals("", textArea.getText()); // no leftover placeholder text
        assertTrue(textArea.isEditable());
        assertTrue(textArea.getLineWrap());
    }

    @Test
    void undoAndRedoKeysAreBound() {
        assertEquals("Undo", textArea.getInputMap().get(KeyStroke.getKeyStroke("control Z")));
        assertEquals("Redo", textArea.getInputMap().get(KeyStroke.getKeyStroke("control Y")));
        assertNotNull(textArea.getActionMap().get("Undo"));
        assertNotNull(textArea.getActionMap().get("Redo"));
    }

    @Test
    void undoRevertsTheLastEditAndRedoBringsItBack() {
        textArea.append("hello");
        textArea.append(" world");
        assertEquals("hello world", textArea.getText());

        runAction("Undo");
        assertEquals("hello", textArea.getText());

        runAction("Redo");
        assertEquals("hello world", textArea.getText());
    }

    @Test
    void undoWithoutAnyEditDoesNothing() {
        assertDoesNotThrow(() -> runAction("Undo"));
        assertDoesNotThrow(() -> runAction("Redo"));
        assertEquals("", textArea.getText());
    }

    @Test
    void ctrlClickWithoutSelectionDoesNotThrow() {
        // regression test: getSelectedText() returns null when nothing is selected
        textArea.setText("GET / HTTP/1.1\r\nHost: example.com");
        textArea.setCaretPosition(2);
        assertDoesNotThrow(() -> dispatchToQuickCopyListener(InputEvent.CTRL_DOWN_MASK));
    }

    @Test
    void ctrlAltClickWithoutSelectionDoesNotThrow() {
        textArea.setText("some text");
        textArea.setCaretPosition(1);
        assertDoesNotThrow(() -> dispatchToQuickCopyListener(InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK));
    }

    @Test
    void getCurrentCursorLineReturnsTheCaretLine() {
        textArea.setText("first line\nsecond line\nthird line");
        textArea.setCaretPosition("first line\nsec".length());
        assertEquals("second line\n", SharpenerMessageEditor_RTextScrollPane.getCurrentCursorLine(textArea));

        textArea.setCaretPosition(textArea.getDocument().getLength());
        assertEquals("third line", SharpenerMessageEditor_RTextScrollPane.getCurrentCursorLine(textArea));
    }

    private void runAction(String actionName) {
        textArea.getActionMap().get(actionName).actionPerformed(new ActionEvent(textArea, ActionEvent.ACTION_PERFORMED, actionName));
    }

    // Sends a left click with the given modifiers to the quick copy/paste listener only,
    // so the internal RSyntaxTextArea mouse handling cannot interfere with the test.
    private void dispatchToQuickCopyListener(int modifiers) {
        MouseEvent event = new MouseEvent(textArea, MouseEvent.MOUSE_PRESSED, 0L, modifiers, 5, 5, 1, false, MouseEvent.BUTTON1);
        boolean listenerFound = false;
        for (MouseListener listener : textArea.getMouseListeners()) {
            if (listener.getClass().getName().contains("SharpenerMessageEditor_RTextScrollPane")) {
                listener.mousePressed(event);
                listenerFound = true;
            }
        }
        assertTrue(listenerFound, "quick copy/paste mouse listener is not installed");
    }
}
