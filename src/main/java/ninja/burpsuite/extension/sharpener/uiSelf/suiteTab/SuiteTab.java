// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
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
