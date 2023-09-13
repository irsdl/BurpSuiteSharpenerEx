// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

import ninja.burpsuite.libs.generic.UIHelper;
import ninja.burpsuite.libs.generic.uiObjFinder.UIWalker;
import ninja.burpsuite.libs.generic.uiObjFinder.UiSpecObject;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class BurpUITools {
    public enum MainTabs {
        None("None"),
        Collaborator("Collaborator"),
        Comparer("Comparer"),
        Dashboard("Dashboard"),
        Decoder("Decoder"),
        Extender("Extender"),
        Extensions("Extensions"),
        Intruder("Intruder"),
        Learn("Learn"),
        Logger("Logger"),
        ProjectOptions("Project options"),
        Proxy("Proxy"),
        Organizer("Organizer"),
        Repeater("Repeater"),
        Sequencer("Sequencer"),
        Target("Target"),
        UserOptions("User options"),

        HackVertor("Extension:Hackvertor"),
        LoggerPlusPlus("Extension:Logger++"),
        PythonScripter("Extension:Python Scripts"),
        Stepper("Extension:Stepper"),
        Autorize("Extension:Autorize"),
        Errors("Extension:Errors"),
        ;
        private final String text;

        MainTabs(final String text) {
            this.text = text.replaceAll("Extension: *", "Extension> ");
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text.replaceAll("Extension> *", "");
        }

        public String toRawString() {
            return text;
        }

    }

    public static MainTabs getMainTabsObjFromString(String tabTitleName) {
        MainTabs result = MainTabs.None;
        if (tabTitleName != null) {
            for (MainTabs tab : BurpUITools.MainTabs.values()) {
                if (tab.toString().equalsIgnoreCase(tabTitleName.trim())) {
                    result = tab;
                    break;
                }
            }
        }
        return result;
    }

    public static boolean isDarkMode(Component component) {
        boolean result = component.getBackground().getBlue() < 128;
        return result;
    }

    public static boolean switchToMainTab(String tabName, JTabbedPane tabbedPane) {
        boolean result = false;
        for (Component component : tabbedPane.getComponents()) {
            int componentIndex = tabbedPane.indexOfComponent(component);
            if (componentIndex == -1) {
                continue;
            }

            String componentTitle = tabbedPane.getTitleAt(componentIndex);
            if (componentTitle.trim().equalsIgnoreCase(tabName.trim())) {
                tabbedPane.setSelectedIndex(componentIndex);
                result = true;
                break;
            }
        }

        return result;
    }

    public static boolean isStringInMainTabs(String tabTitleName) {
        boolean result = true;
        try {
            MainTabs.valueOf(tabTitleName);
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    // This is case-insensitive to prevent confusion
    public static boolean isMenuBarLoaded(String toolbarName, JMenuBar menuBar) {
        boolean result = false;
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenuItem item = menuBar.getMenu(i);
            if (item.getText().trim().equalsIgnoreCase(toolbarName.trim())) {
                result = true;
                break;
            }
        }
        return result;
    }

    // This is case-insensitive to prevent confusion
    public static void removeMenuBarByName(String toolbarName, JMenuBar menuBar, boolean repaintUI) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenuItem item = menuBar.getMenu(i);
            if (item.getText().trim().equalsIgnoreCase(toolbarName.trim())) {
                menuBar.remove(i);
                // break; // we may have more than one menu so this line needs to be commented
            }
        }

        if (repaintUI) {
            menuBar.revalidate();
            menuBar.repaint();
        }
    }

    public static void removeMenuBarByName_noUiLock(String toolbarName, JMenuBar menuBar, boolean repaintUI) {
        SwingUtilities.invokeLater(() -> {
            removeMenuBarByName(toolbarName, menuBar, repaintUI);
        });
    }

    // This is case-insensitive to prevent confusion
    public static JMenuItem getMenuItem(String toolbarName, JMenuBar menuBar) {
        JMenuItem result = null;
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenuItem item = menuBar.getMenu(i);
            if (item.getText().trim().equalsIgnoreCase(toolbarName.trim())) {
                result = item;
                break;
            }
        }
        return result;
    }

    // This is case-insensitive to prevent confusion
    public static MenuElement getSubMenuComponentFromMain(String toolbarName, String subItemName, JMenuBar menuBar) {
        MenuElement result = null;
        JMenuItem mainMenuItem = getMenuItem(toolbarName, menuBar);
        if (mainMenuItem != null) {
            for (int i = 0; i < mainMenuItem.getSubElements()[0].getSubElements().length - 1; i++) {
                MenuElement item = mainMenuItem.getSubElements()[0].getSubElements()[i];
                if (item instanceof JMenuItem finalObj) {
                    if (finalObj.getText().equalsIgnoreCase(subItemName)) {
                        result = finalObj;
                        break;
                    }
                } else if (item instanceof JMenu finalObj) {
                    if (finalObj.getText().equalsIgnoreCase(subItemName)) {
                        result = finalObj;
                        break;
                    }
                } else if (item instanceof JCheckBoxMenuItem finalObj) {
                    if (finalObj.getText().equalsIgnoreCase(subItemName)) {
                        result = finalObj;
                        break;
                    }
                }
            }
        }
        return result;
    }

    // This used to work before right click detach was introduced in burp in 2023
    @Deprecated
    public static boolean reattachToolsLegacy(Set<BurpUITools.MainTabs> toolName, JMenuBar menuBar) {
        boolean result = false;
        for (BurpUITools.MainTabs tool : toolName) {
            JMenuItem detachedTool = (JMenuItem) BurpUITools.getSubMenuComponentFromMain("Window", "Reattach " + tool, menuBar);
            if (detachedTool != null) {
                detachedTool.doClick();
                result = true;
            }
        }
        return result;
    }

    // this does not work anywhere even in the latest version but has been added just in case we need to force it to work in the future!
    @Deprecated
    public static boolean reattachTools(Set<BurpUITools.MainTabs> toolName, JMenuBar menuBar) {
        boolean result = false;
        for (BurpUITools.MainTabs toolTabName : toolName) {
            for (Window window : Window.getWindows()) {
                if (window.isShowing()) {
                    if (window instanceof JFrame) {
                        String title = ((JFrame) window).getTitle();

                        // "Repeater" used to become "Burp Repeater" when it was detached
                        //if (title.equalsIgnoreCase("Burp " + toolTabName.toString())) {

                        // "Repeater" is now "Repeater" (v2023.10)
                        if (title.equalsIgnoreCase(toolTabName.toString())) {
                            // it is not possible to trigger the reattach functionality in v2023.10-22956 (latest atm)
                            UiSpecObject uiSpecObject = new UiSpecObject();
                            uiSpecObject.set_isJComponent(true);
                            uiSpecObject.set_isShowing(true);
                            //uiSpecObject.set_maxJComponentCount(1);
                            uiSpecObject.set_toolTipText("Reattach all tabs");
                            uiSpecObject.set_hasMouseListener(true);
                            Component tempComponent = UIWalker.findUIObjectInSubComponents(window.getComponents()[0], 6, uiSpecObject);
                            if (tempComponent != null) {
                                JLabel label = (JLabel) tempComponent;
                                UIHelper.simulateClick(label);
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return result;

    }

    // This is case-insensitive to prevent confusion
    @Deprecated
    public static boolean isTabLoaded(String tabName, JTabbedPane tabbedPane) {
        boolean result = false;
        for (Component component : tabbedPane.getComponents()) {
            int componentIndex = tabbedPane.indexOfComponent(component);
            if (componentIndex == -1) {
                continue;
            }

            String componentTitle = tabbedPane.getTitleAt(componentIndex);
            if (componentTitle.trim().equalsIgnoreCase(tabName.trim())) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static MainTabs getBurpSuiteToolName(JTabbedPane toolTabbedPane) {
        return getMainTabsObjFromString(toolTabbedPane.getTitleAt(0));
    }
}
