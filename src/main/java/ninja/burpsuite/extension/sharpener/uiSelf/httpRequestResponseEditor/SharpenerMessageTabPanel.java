package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;

public class SharpenerMessageTabPanel extends JPanel {
    ExtensionSharedParameters sharedParameters;

    public SharpenerMessageTabPanel(ExtensionSharedParameters sharedParameters, boolean readOnly) {
        super(new GridBagLayout());
        this.sharedParameters = sharedParameters;

        RSyntaxTextArea textArea = new RSyntaxTextArea(50, 200);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        //textArea.setFont(new Font("LucidaSans", Font.PLAIN, 20));
        // It is a readonly text box
        textArea.setEditable(!readOnly);
        // Set the text that should go to the textbox
        textArea.setText("message");

        // this is to update the UI using the idea from https://github.com/hackvertor/hackvertor/blob/master/src/main/java/burp/ui/HackvertorPanel.java
        // probably I should use the same approach as in https://github.com/hackvertor/hackvertor/blob/master/src/main/java/burp/ui/HackvertorInput.java
        RSyntaxUtils.configureRSyntaxArea(textArea, sharedParameters);

        RSyntaxUtils.fixRSyntaxAreaBurp();

        this.add(textArea);

    }

    public boolean isModified() {
        return false;
    }
}
