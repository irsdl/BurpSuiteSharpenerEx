// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener;

import burp.api.montoya.MontoyaApi;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObject;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObjectStyle;
import ninja.burpsuite.extension.sharpener.uiControllers.subTabs.SubTabsContainerHandler;
import ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenu;
import ninja.burpsuite.libs.burp.generic.BurpExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.burp.generic.BurpUIDiscovery;

import javax.swing.*;
import javax.swing.plaf.TabbedPaneUI;
import java.awt.*;
import java.util.*;

public class ExtensionSharedParameters extends BurpExtensionSharedParameters {
    public TopMenu topMenuBar;
    public HashMap<BurpUITools.MainTabs, ArrayList<SubTabsContainerHandler>> allSubTabContainerHandlers = new HashMap<>();
    private Set<BurpUITools.MainTabs> subTabSupportedTabs = new HashSet<>();
    public HashMap<BurpUITools.MainTabs, HashMap<String, TabFeaturesObject>> supportedTools_SubTabs = new HashMap<>();
    public TabFeaturesObjectStyle defaultTabFeaturesObjectStyle = null;
    public ExtensionGeneralSettings allSettings;
    public TabFeaturesObjectStyle copiedTabFeaturesObjectStyle;
    public String lastClipboardText = "";
    public String searchedTabTitleForPasteStyle = "";
    public String matchReplaceTitle_RegEx = "";
    public String matchReplaceTitle_ReplaceWith = "";
    public String searchedTabTitleForJumpToTab = "";
    public String titleFilterRegEx = "";
    public boolean isTitleFilterNegative = false;
    public boolean isTabGroupSupportedByDefault = false;
    public boolean isSubTabScrollSupportedByDefault = false;
    // Old Burp used the sub-tab background colour as the tab title text colour.
    // New Burp paints the real tab background, so the text colour must be set on the title text field directly.
    public boolean isTabTextColorSetByBackground = true;
    public String burpSupportedColorNames = "Red|Orange|Yellow|Green|Cyan|Blue|Pink|Magenta|Gray"; // from burp.api.montoya.core.HighlightColor
    public HashMap<BurpUITools.MainTabs, Integer> filterOperationMode = new HashMap<>();
    public HashMap<BurpUITools.MainTabs, LinkedList<Integer>> subTabPreviouslySelectedIndexHistory = new HashMap<>();
    public HashMap<BurpUITools.MainTabs, LinkedList<Integer>> subTabNextlySelectedIndexHistory = new HashMap<>();
    public HashMap<BurpUITools.MainTabs, TabbedPaneUI> originalSubTabbedPaneUI = new HashMap<>();

    public ExtensionSharedParameters(ExtensionMainClass extensionMainClass, MontoyaApi api, String propertiesFilePath) {
        super(extensionMainClass, api, propertiesFilePath);
        loadingChecks();
    }

    private void loadingChecks() {
        // do stuff such as setting an initial parameter based on Burp suite version or its title etc.
        // all supported Burp versions (2024.2 and later) have these features
        this.isTabGroupSupportedByDefault = true;
        this.isSubTabScrollSupportedByDefault = true;

        this.isTabTextColorSetByBackground = isTabTextColorSetByBackgroundForVersion(burpMajorVersion);

        subTabSupportedTabs.add(BurpUITools.MainTabs.Repeater);
        subTabSupportedTabs.add(BurpUITools.MainTabs.Intruder);

        for (BurpUITools.MainTabs supportedTabs : subTabSupportedTabs) {
            supportedTools_SubTabs.put(supportedTabs, new HashMap<>());
            filterOperationMode.put(supportedTabs, 0);
            subTabPreviouslySelectedIndexHistory.put(supportedTabs, new LinkedList<>());
            subTabNextlySelectedIndexHistory.put(supportedTabs, new LinkedList<>());
        }
    }

    // Burp changed the sub-tab design around version 2026.
    // Since then, the tab background colour is really painted as a background.
    // Change this version check if the exact change point turns out to be different.
    public static boolean isTabTextColorSetByBackgroundForVersion(double burpMajorVersion) {
        return burpMajorVersion < 2026;
    }

    // A method to get list of subTabSupportedTabs which are accessible
    public Set<BurpUITools.MainTabs> getAccessibleSubTabSupportedTabs() {
        Set<BurpUITools.MainTabs> finalSubTabSupportedTabs = new HashSet<>();
        // for each item in subTabSupportedTabs check if it is accessible
        for (BurpUITools.MainTabs tool : subTabSupportedTabs) {
            if (get_toolTabbedPane(tool) != null) {
                finalSubTabSupportedTabs.add(tool);
            }
        }
        return finalSubTabSupportedTabs;
    }

    // A method to get list of all subTabSupportedTabs regardless of their accessibility
    public Set<BurpUITools.MainTabs> getAllSubTabSupportedTabs() {
        return subTabSupportedTabs;
    }

    public boolean isFiltered(BurpUITools.MainTabs toolTabName) {
        return getHiddenSubTabsCount(toolTabName) > 0;
    }

    public int getHiddenSubTabsCount(BurpUITools.MainTabs toolTabName) {
        if (allSubTabContainerHandlers.get(toolTabName) == null)
            return -1;
        else
            return allSubTabContainerHandlers.get(toolTabName).stream().filter(s -> s.isValid() && !s.getVisible()).toArray().length;
    }

    public JTabbedPane get_toolTabbedPane(BurpUITools.MainTabs toolTabName) {
        return get_toolTabbedPane(toolTabName, true);
    }

    public JTabbedPane get_toolTabbedPane(BurpUITools.MainTabs toolTabName, boolean useCache) {
        // Re-discover within the current tool subtree: a detached/replaced pane can remain
        // perfectly callable, so getSelectedComponent() is not evidence a cached pane is live.
        return BurpUIDiscovery.onEdt(() -> {
            if (toolTabName == null || toolTabName == BurpUITools.MainTabs.None) return null;
            Component content = BurpUIDiscovery.toolContent(get_rootTabbedPaneUsingMontoya(), toolTabName);
            if (content != null) return BurpUIDiscovery.findToolTabs(content);

            JTabbedPane detached = null;
            for (Window window : Window.getWindows()) {
                if (window.isShowing() && window instanceof JFrame frame
                        && (toolTabName.toString().equalsIgnoreCase(frame.getTitle())
                        || ("Burp " + toolTabName).equalsIgnoreCase(frame.getTitle()))) {
                    JTabbedPane candidate = BurpUIDiscovery.findToolTabs(frame.getContentPane());
                    if (candidate != null) {
                        if (detached != null) return null;
                        detached = candidate;
                    }
                }
            }
            return detached;
        });
    }

}
