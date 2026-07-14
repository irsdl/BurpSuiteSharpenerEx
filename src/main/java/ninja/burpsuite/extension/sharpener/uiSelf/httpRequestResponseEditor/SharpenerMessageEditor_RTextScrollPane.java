package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SharpenerMessageEditor_RTextScrollPane extends RTextScrollPane {
    private final RSyntaxTextArea textArea;

    public SharpenerMessageEditor_RTextScrollPane(ExtensionSharedParameters sharedParameters) {
        super(createTextArea(sharedParameters), true);
        textArea = (RSyntaxTextArea) getViewport().getView();
        textArea.setEditable(true);
    }

    private static RSyntaxTextArea createTextArea(ExtensionSharedParameters sharedParameters) {
        RSyntaxTextArea textArea = new SharpenerMessageEditor_RSyntaxTextArea(sharedParameters);
        textArea.setLineWrap(true);
        textArea.setEditable(true);
        installUndoRedo(textArea);
        installQuickCopyPaste(sharedParameters, textArea);
        return textArea;
    }

    // The shared RTextAreaKeymap is removed so it cannot override Burp shortcuts,
    // and that also removes the built-in undo/redo keys, so they are added back here.
    private static void installUndoRedo(RSyntaxTextArea textArea) {
        UndoManager undo = new UndoManager();
        Document doc = textArea.getDocument();
        doc.addUndoableEditListener(evt -> undo.addEdit(evt.getEdit()));

        textArea.getActionMap().put("Undo", new AbstractAction("Undo") {
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (CannotUndoException ignored) {
                }
            }
        });
        textArea.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");

        textArea.getActionMap().put("Redo", new AbstractAction("Redo") {
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException ignored) {
                }
            }
        });
        textArea.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
    }

    // Experimental: Ctrl+Click copies the selected text or the current line,
    // Ctrl+Alt+Click pastes the clipboard over the selected text.
    private static void installQuickCopyPaste(ExtensionSharedParameters sharedParameters, RSyntaxTextArea textArea) {
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                boolean isCtrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
                boolean isAlt = (e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK;
                if (!isCtrl) {
                    return;
                }

                String selectedText = textArea.getSelectedText(); // null when nothing is selected

                if (isAlt) {
                    // Ctrl+Alt+Click
                    printDebug(sharedParameters, "Ctrl+Alt+Click performed");

                    if (selectedText != null && !selectedText.isEmpty()) {
                        textArea.replaceSelection(pasteFromClipboard(sharedParameters));
                    }
                    // TODO: when nothing is selected, check if the cursor position is on a HTTP header or body
                    //  if it is on body, then paste the clipboard content where the cursor is
                    //  check if the cursor is on a header or body by splitting the content by \n and checking if the cursor is on a line with a colon
                    //  if it is on a header line, then if the copied value has a colon, read the header name and search
                } else {
                    // Ctrl+Click
                    printDebug(sharedParameters, "Ctrl+Click performed");

                    // TODO: introduce Ctrl+Shift+Click to copy based on patterns around the cursor position
                    //  e.g. if it is in a uuid or a number surrounded by non alphanumeric characters, copy the number then create the object we have here
                    //  or if it is a text surrounded by quotes or spaces etc.
                    // TODO: when copying into clipboard, it would be good to use an object which can also store
                    //  0- if the text has been copied from ctrl+click or ctrl+shift+click or if it is copied from outside in the clipboard
                    //  1- if it was a highlighted text or just a cursor position
                    //  1.1- we need to store the position of the cursor or the highlighted text in the textArea
                    //  1.2- we need to store some information about the source of the text, e.g. if it is from a response or a request
                    //  Note: set-cookie from a response pasted in a request should be handled differently for example
                    //  2- the position type it was copied from, e.g. first-line, header, body.
                    //  2.1- for first-line whether the cursor or selected text was: in path, query, fragment, or method
                    //  2.2- for header whether the cursor or selected text was: in header name or header value
                    //  2.2.1- if it is from a header, we need to also store the header name and header value
                    //  2.2.2- if it is in a cookie header in a cookie name or value, we need to also store the cookie name and cookie value
                    //  2.3- for body, we need to store the body type (content-type)
                    //  2.3.1- we need to know whether the cursor was in a json key or value, or a xml tag or value or attribute name or attribute value, or a form-data key or value
                    //  for json and xml, we may need to know the depth (xpath or json path) of the cursor
                    //  note: highlighted text when it is covering multiple parameters like in query, cookies, body (normal, json, xml) can be tricky to process when pasting
                    //  note: when pasting a value that could have been repeated more than once, we need to know if we should replace all or just the closest one

                    if (selectedText != null && !selectedText.isEmpty()) {
                        copyToClipboard(sharedParameters, selectedText);
                    } else {
                        copyToClipboard(sharedParameters, getCurrentCursorLine(textArea));
                    }
                }
                e.consume(); // stop further processing of this click
            }
        });
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    private static void copyToClipboard(ExtensionSharedParameters sharedParameters, String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (Exception ex) {
            printDebug(sharedParameters, "Error copying to the clipboard: " + ex.getMessage());
        }
    }

    private static String pasteFromClipboard(ExtensionSharedParameters sharedParameters) {
        String clipboardText = "";
        try {
            clipboardText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            printDebug(sharedParameters, "Error reading from the clipboard: " + ex.getMessage());
        }
        return clipboardText;
    }

    // Returns the full text of the line the caret is on, or an empty string on failure.
    static String getCurrentCursorLine(RSyntaxTextArea textArea) {
        String lineText = "";
        try {
            int caretPos = textArea.getCaretPosition();
            int lineNum = textArea.getLineOfOffset(caretPos);
            int startOffset = textArea.getLineStartOffset(lineNum);
            int endOffset = textArea.getLineEndOffset(lineNum);
            lineText = textArea.getText(startOffset, endOffset - startOffset);
        } catch (Exception ex) {
            // keep the empty string
        }
        return lineText;
    }

    private static void printDebug(ExtensionSharedParameters sharedParameters, String message) {
        if (sharedParameters != null) {
            sharedParameters.printDebugMessage(message);
        }
    }
}
