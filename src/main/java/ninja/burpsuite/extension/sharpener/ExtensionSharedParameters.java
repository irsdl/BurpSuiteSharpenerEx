// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener;

import burp.api.montoya.MontoyaApi;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObject;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObjectStyle;
import ninja.burpsuite.extension.sharpener.uiControllers.subTabs.SubTabsContainerHandler;
import ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenu;
import ninja.burpsuite.libs.burp.generic.BurpExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.uiObjFinder.UIWalker;
import ninja.burpsuite.libs.generic.uiObjFinder.UiSpecObject;

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
    public HashMap<BurpUITools.MainTabs, Integer> filterOperationMode = new HashMap<>();
    public HashMap<BurpUITools.MainTabs, LinkedList<Integer>> subTabPreviouslySelectedIndexHistory = new HashMap<>();
    public HashMap<BurpUITools.MainTabs, LinkedList<Integer>> subTabNextlySelectedIndexHistory = new HashMap<>();
    public HashMap<BurpUITools.MainTabs, TabbedPaneUI> originalSubTabbedPaneUI = new HashMap<>();
    private final HashMap<BurpUITools.MainTabs, JTabbedPane> cachedJTabbedPaneTools = new HashMap<>(); // This will keep pointer to the current repeater or intruder even when they are detached

    public ExtensionSharedParameters(ExtensionMainClass extensionMainClass, MontoyaApi api, String propertiesFilePath) {
        super(extensionMainClass, api, propertiesFilePath);
        loadingChecks();
    }

    private void loadingChecks() {
        // do stuff such as setting an initial parameter based on Burp suite version or its title etc.
        if ((burpMajorVersion >= 2022 && burpMinorVersion >= 6) || burpMajorVersion >= 2023) {
            this.isTabGroupSupportedByDefault = true;
            this.isSubTabScrollSupportedByDefault = true;
        }

        subTabSupportedTabs.add(BurpUITools.MainTabs.Repeater);
        subTabSupportedTabs.add(BurpUITools.MainTabs.Intruder);

        for (BurpUITools.MainTabs supportedTabs : subTabSupportedTabs) {
            supportedTools_SubTabs.put(supportedTabs, new HashMap<>());
            filterOperationMode.put(supportedTabs, 0);
            subTabPreviouslySelectedIndexHistory.put(supportedTabs, new LinkedList<>());
            subTabNextlySelectedIndexHistory.put(supportedTabs, new LinkedList<>());
        }
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
        JTabbedPane toolTabbedPane = null;
        JTabbedPane _rootTabbedPane = get_rootTabbedPaneUsingMontoya();


        if (useCache && cachedJTabbedPaneTools.get(toolTabName) != null) {
            toolTabbedPane = cachedJTabbedPaneTools.get(toolTabName);
            try {
                toolTabbedPane.getSelectedComponent();
            } catch (Exception e) {
                // could not access the object
                toolTabbedPane = null;
            }
        }

        if (_rootTabbedPane != null && toolTabbedPane == null) {
            boolean hasSubTabs = true;
            for (Component tabComponent : _rootTabbedPane.getComponents()) {

                //Check tab titles and continue for accepted tab paths.
                int componentIndex = _rootTabbedPane.indexOfComponent(tabComponent);
                if (componentIndex == -1) {
                    continue;
                }
                String componentTitle = _rootTabbedPane.getTitleAt(componentIndex);

                if (toolTabName.toString().equalsIgnoreCase(componentTitle)) {
                    // we have our tool tab, now we need to find its right component
                    try {
                        toolTabbedPane = (JTabbedPane) tabComponent;
                    } catch (Exception e1) {
                        try {
                            toolTabbedPane = (JTabbedPane) ((JComponent) tabComponent).getComponents()[0];
                            //toolTabbedPane = (JTabbedPane) tabComponent.getComponentAt(0, 0); // this is not working when repeater or intruder do not have any tabs
                        } catch (Exception e2) {
                            hasSubTabs = false;
                            printDebugMessage("The " + componentTitle + " tool seems to be empty or different. Cannot find the tabs.");
                        }
                    }
                    break;
                }
            }

            if (toolTabbedPane == null && hasSubTabs) {
                // it could not find the tool, this can happen when a tool has been detached, so we need to look for it!
                for (Window window : Window.getWindows()) {
                    if (window.isShowing()) {
                        if (window instanceof JFrame) {
                            String title = ((JFrame) window).getTitle();

                            // "Repeater" used to become "Burp Repeater" when it was detached
                            //if (title.equalsIgnoreCase("Burp " + toolTabName.toString())) {

                            // "Repeater" is now "Repeater" (v2023.10)
                            if (title.equalsIgnoreCase(toolTabName.toString())) {
                                UiSpecObject uiSpecObject = new UiSpecObject(JTabbedPane.class);
                                uiSpecObject.set_isJComponent(true);
                                uiSpecObject.set_isShowing(true);

                                Component tempComponent = UIWalker.findUIObjectInSubComponents(window.getComponents()[0], 6, uiSpecObject);
                                // after finding the first JTabbedPane, we need to look again in its children and exclude itself to find the one that includes Repeater or Intruder tabs
                                Component tempComponent2 = UIWalker.findUIObjectInSubComponentsWithExclusions(tempComponent, 1, uiSpecObject, new Component[]{tempComponent});
                                if (tempComponent2 != null) {
                                    toolTabbedPane = (JTabbedPane) tempComponent2;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (toolTabbedPane != null) {
            if (cachedJTabbedPaneTools.get(toolTabName) != null) {
                cachedJTabbedPaneTools.replace(toolTabName, toolTabbedPane);
            } else {
                cachedJTabbedPaneTools.put(toolTabName, toolTabbedPane);
            }
        }

        if (toolTabbedPane == null) {
            printDebugMessage("subTabbedPane is null for " + toolTabName + ". This can cause an error if not handled gracefully.");
        }

        return toolTabbedPane;
    }

}
