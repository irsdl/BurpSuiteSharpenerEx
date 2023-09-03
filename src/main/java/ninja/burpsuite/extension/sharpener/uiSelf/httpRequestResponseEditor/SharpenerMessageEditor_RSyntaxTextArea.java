// initial code from https://github.com/hackvertor/hackvertor/blob/master/src/main/java/burp/ui/HackvertorInput.java
package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SharpenerMessageEditor_RSyntaxTextArea extends RSyntaxTextArea {

    ExtensionSharedParameters sharedParameters;

    public SharpenerMessageEditor_RSyntaxTextArea(ExtensionSharedParameters sharedParameters) {
        super();
        this.sharedParameters = sharedParameters;

        SharpenerMessageEditor_RSyntaxTextArea that = this;
        this.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_EQUALS) && (e.isMetaDown() || (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
                    int fontSize = that.getFont().getSize();
                    that.changeFontSize(fontSize + 1);
                } else if ((e.getKeyCode() == KeyEvent.VK_MINUS) && (e.isMetaDown() || (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
                    int fontSize = that.getFont().getSize();
                    that.changeFontSize(fontSize - 1);
                } else if ((e.isControlDown() || e.isMetaDown()) && (e.getKeyCode() == KeyEvent.VK_0)) {
                    getFontSizeFromBurp();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
    }


    public void updateUI() {
        super.updateUI();

        SwingUtilities.invokeLater(() -> {
            if(sharedParameters.montoyaApi.userInterface().currentTheme().name().equalsIgnoreCase("dark")) {
                RSyntaxUtils.applyThemeToRSyntaxTextArea(this, "dark", sharedParameters);
            } else {
                RSyntaxUtils.applyThemeToRSyntaxTextArea(this, "default", sharedParameters);
            }
            getFontSizeFromBurp();
        });
    }

    public void getFontSizeFromBurp() {
        sharedParameters.montoyaApi.userInterface().applyThemeToComponent(this);
        this.changeFontSize(this.getFont().getSize());
    }

    public void changeFontSize(int fontSize) {
        Font currentFont = sharedParameters.montoyaApi.userInterface().currentEditorFont();
        this.setFont(new Font(currentFont.getFontName(), currentFont.getStyle(), fontSize));
        sharedParameters.montoyaApi.userInterface().applyThemeToComponent(this);
    }
}
