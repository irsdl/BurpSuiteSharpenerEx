// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiSelf.suiteTab;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import javax.swing.*;

public class SuiteTab extends JComponent {
    private static final long serialVersionUID = 1L;
    transient ExtensionSharedParameters sharedParameters;

    public SuiteTab(ExtensionSharedParameters sharedParameters) {
        this.sharedParameters = sharedParameters;
    }
}
