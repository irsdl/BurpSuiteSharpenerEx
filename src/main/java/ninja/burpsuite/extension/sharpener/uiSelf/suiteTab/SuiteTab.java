// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.uiSelf.suiteTab;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import javax.swing.*;

public class SuiteTab extends JComponent {
    ExtensionSharedParameters sharedParameters;

    public SuiteTab(ExtensionSharedParameters sharedParameters) {
        this.sharedParameters = sharedParameters;
    }
}
