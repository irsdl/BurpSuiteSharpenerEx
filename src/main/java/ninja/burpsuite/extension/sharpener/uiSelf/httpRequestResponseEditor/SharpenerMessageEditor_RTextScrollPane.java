package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class SharpenerMessageEditor_RTextScrollPane extends RTextScrollPane {
    private RSyntaxTextArea textArea;

    public SharpenerMessageEditor_RTextScrollPane(ExtensionSharedParameters sharedParameters) {
        super(createTextArea(sharedParameters), true);
        // it seems that RTextScrollPane is not working properly with RSyntaxTextArea and makes it readonly:
        // editor is not editable
        // there is no scroll bar on right side!

        textArea = (RSyntaxTextArea) getViewport().getView();
        textArea.setEditable(true);
    }

    private static RSyntaxTextArea createTextArea(ExtensionSharedParameters sharedParameters) {
        RSyntaxTextArea textArea = new SharpenerMessageEditor_RSyntaxTextArea(sharedParameters);
        //RSyntaxUtils.fixRSyntaxAreaBurp();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        //sharpenerMessageEditor_RSyntaxTextArea.setAntiAliasingEnabled(true);
        textArea.setEditable(true);
        textArea.setText("test message test message test message test message test message test message test message test message test message ");
        //sharpenerMessageEditor_RSyntaxTextArea.setPreferredSize(new Dimension(100, 20));
        //sharpenerMessageEditor_RSyntaxTextArea.setMinimumSize(new Dimension(-1, 100));
        //sharpenerMessageEditor_RSyntaxTextArea.setCodeFoldingEnabled(true);

        return textArea;
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }
}
