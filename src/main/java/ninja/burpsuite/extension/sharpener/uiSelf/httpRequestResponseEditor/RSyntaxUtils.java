// initial code from https://github.com/hackvertor/hackvertor/blob/master/src/main/java/burp/Utils.java

package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

import javax.swing.*;
import java.io.IOException;

public class RSyntaxUtils {

    public static void applyThemeToRSyntaxTextArea(RSyntaxTextArea area, String themeName, ExtensionSharedParameters sharedParameters) {
        try {
            Theme theme = Theme.load(RSyntaxUtils.class.getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/"+themeName+".xml"));
            theme.apply(area);
        } catch (IOException ioe) {
            sharedParameters.printException(ioe);
        }
    }

    public static void fixRSyntaxAreaBurp() {
        UIManager.put("RSyntaxTextAreaUI.actionMap", null);
        UIManager.put("RSyntaxTextAreaUI.inputMap", null);
        UIManager.put("RTextAreaUI.actionMap", null);
        UIManager.put("RTextAreaUI.inputMap", null);
    }

    public static void configureRSyntaxArea(RSyntaxTextArea area, ExtensionSharedParameters sharedParameters) {
        //area.setLineWrap(true);
        if(sharedParameters.montoyaApi.userInterface().currentTheme().name().equalsIgnoreCase("dark")) {
            RSyntaxUtils.applyThemeToRSyntaxTextArea(area, "dark", sharedParameters);
        }
        sharedParameters.montoyaApi.userInterface().applyThemeToComponent(area);
    }

}
