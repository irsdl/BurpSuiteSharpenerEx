// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.google.common.io.Files;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObjectStyle;
import ninja.burpsuite.extension.sharpener.uiControllers.shortcuts.ShortcutMappings;
import ninja.burpsuite.extension.sharpener.uiControllers.shortcuts.ShortcutsDialog;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.*;
import ninja.burpsuite.libs.generic.uiObjFinder.UIWalker;
import ninja.burpsuite.libs.generic.uiObjFinder.UiSpecObject;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class SubTabsActions {

    // the bundled tab icons shown in the tab menu, and their menu display width
    public static final String SUB_TAB_ICON_FOLDER = "subtabicons";
    public static final int SUB_TAB_ICON_MENU_WIDTH = 32;

    // True while Sharpener changes the selected tab itself. The tab selection history
    // recorder checks this so a Sharpener jump is not recorded twice.
    private static boolean tabJumpInProgress = false;

    public static boolean isTabJumpInProgress() {
        return tabJumpInProgress;
    }

    public static void tabClicked(final MouseEvent event, ExtensionSharedParameters sharedParameters) {
        SubTabsContainerHandler subTabsContainerHandler = getSubTabContainerHandlerFromEvent(sharedParameters, event);

        if (subTabsContainerHandler == null) {
            sharedParameters.printlnError("This tab has not been detected yet. It will be loaded shortly, please try again in a few seconds.");
        }

        if (subTabsContainerHandler == null || (!subTabsContainerHandler.isValid() && !subTabsContainerHandler.isDotDotDotTab()))
            return;

        subTabsContainerHandler.currentTabContainer.requestFocus();

        fixHistoryAndJumpToTabIndex(sharedParameters, subTabsContainerHandler, subTabsContainerHandler.getTabIndex(), true, true, false);

        if (SwingUtilities.isMiddleMouseButton(event) || event.isAltDown()) {
            jumpToTabIndex(sharedParameters, subTabsContainerHandler, subTabsContainerHandler.getTabIndex());
            boolean isCTRL_Key = event.isControlDown();
            // Middle key is like the Alt key!
            boolean isSHIFT_Key = event.isShiftDown();

            int maxSize = 40;
            int minSize = 10;
            if (!isCTRL_Key && !isSHIFT_Key) {
                // showing popup menu
                showPopupMenu(sharedParameters, subTabsContainerHandler, event);
            } else if (isCTRL_Key && !isSHIFT_Key) {
                // Make it bigger and bold when middle click + ctrl
                if (subTabsContainerHandler.getFontSize() < maxSize) {
                    if (!subTabsContainerHandler.isBold())
                        subTabsContainerHandler.toggleBold(false);
                    subTabsContainerHandler.setFontSize(subTabsContainerHandler.getFontSize() + 2, false);
                    subTabsContainerHandler.hideCloseButton(false);
                }
            } else if (isCTRL_Key) {
                // Make it smaller but bold when middle click + ctrl + shift
                if (subTabsContainerHandler.getFontSize() > minSize) {
                    if (!subTabsContainerHandler.isBold())
                        subTabsContainerHandler.toggleBold(false);
                    subTabsContainerHandler.setFontSize(subTabsContainerHandler.getFontSize() - 2, false);
                    subTabsContainerHandler.hideCloseButton(false);
                }
            } else {
                // middle click with shift: should make it red and big and bold
                TabFeaturesObjectStyle tabFeaturesObjectStyle = new TabFeaturesObjectStyle("High: Red, Big, and Bold", "Arial", 18, true, false, false, Color.decode("#f71414"), "high", 18);
                subTabsContainerHandler.updateByTabFeaturesObjectStyle(tabFeaturesObjectStyle, false);
            }

            if (subTabsContainerHandler.getHasChanges()) {
                sharedParameters.allSettings.subTabsSettings.saveSettings(subTabsContainerHandler);
            }
        }
    }

    public static void addMouseWheelToJTabbedPane(ExtensionSharedParameters sharedParameters, BurpUITools.MainTabs currentToolTab, boolean isLastOneSelectable) {
        // from https://stackoverflow.com/questions/38463047/use-mouse-to-scroll-through-tabs-in-jtabbedpane

        Consumer<MouseWheelEvent> mwl = e -> {
            JTabbedPane tabbedPane = (JTabbedPane) e.getSource();
            // works with version 2022.1.1 - not tested in the previous versions!
            int currentSelection = tabbedPane.getSelectedIndex();
            SubTabsContainerHandler subTabsContainerHandler = getSubTabContainerHandlerFromSharedParameters(sharedParameters, tabbedPane, currentSelection);

            if (subTabsContainerHandler == null)
                return;

            if (e.isControlDown()) {
                float currentFontSize = subTabsContainerHandler.getFontSize();

                if (e.getWheelRotation() < 0) {
                    //scrolled up
                    if (currentFontSize <= 36) {
                        subTabsContainerHandler.setFontSize(currentFontSize + 2, false);
                    }
                } else {
                    //scrolled down
                    if (currentFontSize >= 12) {
                        subTabsContainerHandler.setFontSize(currentFontSize - 2, false);
                    }
                }

                if (subTabsContainerHandler.getHasChanges()) {
                    sharedParameters.allSettings.subTabsSettings.saveSettings(subTabsContainerHandler);
                }
            } else if (e.isAltDown()) {    // experiment here
                subTabsContainerHandler.setIcon("alert", 16, true);
                sharedParameters.allSettings.subTabsSettings.saveSettings(subTabsContainerHandler);

            } else {
                e.isAltDown();
                if (false) { // mw+alt has been disabled as moved tabs won't be saved in the project file!
                    JComponent[] components = new JComponent[2];
                    JComponent[] tabComponents = new JComponent[2];
                    components[0] = (JComponent) tabbedPane.getSelectedComponent();
                    tabComponents[0] = (JComponent) tabbedPane.getTabComponentAt(currentSelection);


                    if (e.getWheelRotation() > 0) {
                        //scrolled down
                        int maxIndex = tabbedPane.getTabCount() - 2;
                        if (sharedParameters.isTabGroupSupportedByDefault)
                            maxIndex += 1;

                        if (currentSelection < maxIndex) {
                            components[1] = (JComponent) tabbedPane.getComponentAt(currentSelection + 1);
                            tabComponents[1] = (JComponent) tabbedPane.getTabComponentAt(currentSelection + 1);

                            //*
                            try {
                                tabbedPane.remove(currentSelection + 1);
                            } catch (Exception err) {

                            }

                            try {
                                tabbedPane.remove(currentSelection);
                            } catch (Exception err) {

                            }

                            try {
                                tabbedPane.add(components[1], currentSelection);

                            } catch (Exception err) {

                            } finally {
                                tabbedPane.setTabComponentAt(currentSelection, tabComponents[1]);
                            }

                            try {
                                tabbedPane.add(components[0], currentSelection + 1);
                            } catch (Exception err) {

                            } finally {
                                tabbedPane.setTabComponentAt(currentSelection + 1, tabComponents[0]);
                            }
                            //*/

                            /*

                            tabbedPane.add(components[1], currentSelection);
                            tabbedPane.add(components[0], currentSelection+1);

                             */

                            // Null Exception from Burp modules!!! :'(
                            //tabbedPane.add(((JComponent) tabbedPane.getTabComponentAt(currentSelection)).getComponent(0), currentSelection+1);
                            //tabbedPane.add(tabbedPane.getTabComponentAt(currentSelection+1), currentSelection);

                            // Null Exception from Burp modules!!! :'(
                            //tabbedPane.insertTab(((JTextField)tabComponents[0].getComponent(0)).getText(),null,components[0],"",currentSelection+1);
                            //tabbedPane.insertTab(((JTextField)tabComponents[1].getComponent(0)).getText(),null,components[1],"",currentSelection);
    /*
                            try{
                                tabbedPane.setComponentAt(currentSelection, components[1]);
                            }catch(Exception err){

                            }finally {
                                tabbedPane.setTabComponentAt(currentSelection, tabComponents[1]);
                            }

                            try{
                                tabbedPane.setComponentAt(currentSelection+1, components[0]);
                            }catch(Exception err){

                            }finally {
                                tabbedPane.setTabComponentAt(currentSelection+1, tabComponents[0]);
                            }


    */
                            jumpToTabIndex(sharedParameters, subTabsContainerHandler, currentSelection + 1);


                            tabbedPane.revalidate();
                            tabbedPane.repaint();
                        }
                    } else {
                        //scrolled up
                        if (currentSelection > 0) {
                            components[1] = (JComponent) tabbedPane.getComponentAt(currentSelection - 1);
                            tabComponents[1] = (JComponent) tabbedPane.getTabComponentAt(currentSelection - 1);
                            //*
                            try {
                                tabbedPane.remove(currentSelection);
                            } catch (Exception err) {

                            }

                            try {
                                tabbedPane.remove(currentSelection - 1);
                            } catch (Exception err) {

                            }

                            try {
                                tabbedPane.add(components[0], currentSelection - 1);
                            } catch (Exception err) {

                            } finally {
                                tabbedPane.setTabComponentAt(currentSelection - 1, tabComponents[0]);
                            }

                            try {
                                tabbedPane.add(components[1], currentSelection);
                            } catch (Exception err) {

                            } finally {
                                tabbedPane.setTabComponentAt(currentSelection, tabComponents[1]);
                            }


                            // */


                            // Null Exception from Burp modules!!! :'(
                            //tabbedPane.add(((JComponent) tabbedPane.getTabComponentAt(currentSelection)).getComponent(0), currentSelection+1);
                            //tabbedPane.add(tabbedPane.getTabComponentAt(currentSelection+1), currentSelection);

                            // Null Exception from Burp modules!!! :'(
                            //tabbedPane.insertTab(((JTextField)tabComponents[0].getComponent(0)).getText(),null,components[0],"",currentSelection+1);
                            //tabbedPane.insertTab(((JTextField)tabComponents[1].getComponent(0)).getText(),null,components[1],"",currentSelection);
    /*
                            try{
                                tabbedPane.setComponentAt(currentSelection, components[1]);
                            }catch(Exception err){

                            }finally {
                                tabbedPane.setTabComponentAt(currentSelection, tabComponents[1]);
                            }

                            try{
                                tabbedPane.setComponentAt(currentSelection-1, components[0]);
                            }catch(Exception err){

                            }finally {
                                tabbedPane.setTabComponentAt(currentSelection-1, tabComponents[0]);
                            }

    */


                            jumpToTabIndex(sharedParameters, subTabsContainerHandler, currentSelection - 1);


                            tabbedPane.revalidate();
                            tabbedPane.repaint();
                        }
                    }
                } else {
                    int offset = 0;
                    if (!isLastOneSelectable)
                        offset = 1;

                    int units = e.getWheelRotation();
                    int oldIndex = tabbedPane.getSelectedIndex();
                    int newIndex = oldIndex + units;
                    int chosenOne = newIndex;
                    int maxIndex = tabbedPane.getTabCount() - offset;

                    if (newIndex < 0)
                        chosenOne = 0;
                    else if (newIndex >= maxIndex)
                        chosenOne = maxIndex - 1;

                    SubTabsContainerHandler chosenOneSubTabsContainerHandler = getSubTabContainerHandlerFromSharedParameters(sharedParameters, tabbedPane, chosenOne);

                    while (chosenOneSubTabsContainerHandler == null || !tabbedPane.isEnabledAt(chosenOne) || !chosenOneSubTabsContainerHandler.isValid()
                            || chosenOneSubTabsContainerHandler.isGroupContainerTab() || !chosenOneSubTabsContainerHandler.isTitleVisible()) {
                        if (units > 0) {
                            //scroll down
                            chosenOne++;
                        } else {
                            //scroll up
                            chosenOne--;
                        }

                        int maxIndex2 = tabbedPane.getTabCount() - offset;

                        if (chosenOne < 0 || chosenOne >= maxIndex2) {
                            chosenOne = oldIndex;
                            break;
                        }
                        chosenOneSubTabsContainerHandler = getSubTabContainerHandlerFromSharedParameters(sharedParameters, tabbedPane, chosenOne);
                    }
                    jumpToTabIndex(sharedParameters, subTabsContainerHandler, chosenOne);
                }
            }

        };
        var currentToolTabbedPane = sharedParameters.get_toolTabbedPane(currentToolTab);
        if (currentToolTabbedPane != null)
            attachMouseWheelHandler(currentToolTabbedPane, mwl);
    }

    public static void removeMouseWheelFromJTabbedPane(ExtensionSharedParameters sharedParameters, BurpUITools.MainTabs currentToolTab) {
        var currentToolTabbedPane = sharedParameters.get_toolTabbedPane(currentToolTab);
        if (currentToolTabbedPane != null) {
            detachMouseWheelHandlers(currentToolTabbedPane);
        }
    }

    // Adds the wheel handler with a named type. Any old handler of the same type is
    // removed first, so repeated calls never stack listeners on the tabbed pane.
    static void attachMouseWheelHandler(JTabbedPane tabbedPane, Consumer<MouseWheelEvent> mouseWheelEventConsumer) {
        detachMouseWheelHandlers(tabbedPane);
        tabbedPane.addMouseWheelListener(new MouseWheelListenerExtensionHandler(mouseWheelEventConsumer));
    }

    // Removes only the wheel listeners added by this extension. The tabbed pane can
    // also carry wheel listeners owned by Burp or the look and feel, those must stay,
    // and ours must not be left behind after unload.
    static void detachMouseWheelHandlers(JTabbedPane tabbedPane) {
        for (MouseWheelListener mouseWheelListener : tabbedPane.getMouseWheelListeners()) {
            if (mouseWheelListener instanceof MouseWheelListenerExtensionHandler) {
                tabbedPane.removeMouseWheelListener(mouseWheelListener);
            }
        }
    }

    private static void setNotificationMenuMessage(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, JMenuItem notificationMenuItem, String message) {
        if (currentSubTabsContainerHandler == null)
            return;

        if (sharedParameters.isFiltered(currentSubTabsContainerHandler.currentToolTab)) {

            if (!currentSubTabsContainerHandler.getVisible()) {
                message = "Filter: ON (" + sharedParameters.getHiddenSubTabsCount(currentSubTabsContainerHandler.currentToolTab) +
                        " hidden tabs) | THIS IS A HIDDEN TAB | " + message;
            } else {
                message = "Filter: ON (" + sharedParameters.getHiddenSubTabsCount(currentSubTabsContainerHandler.currentToolTab) +
                        " hidden tabs) | " + message;
            }

        }
        notificationMenuItem.setText(message);
    }

    private static JPopupMenu createPopupMenu(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler == null)
            return new JPopupMenu();

        // a scrollable menu, so the last items are not lost on short screens;
        // when everything fits, the scrollbar stays hidden and it behaves like a normal menu.
        // No row cap is set here: the menu measures itself against the screen when shown.
        JScrollPopupMenu popupMenu = new JScrollPopupMenu();
        popupMenu.setMaximumVisibleRows(Integer.MAX_VALUE);

        JMenuItem notificationMenuItem = new JMenuItem();
        notificationMenuItem.setFont(notificationMenuItem.getFont().deriveFont(notificationMenuItem.getFont().getStyle() ^ Font.BOLD));
        setNotificationMenuMessage(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem, "Tab Title: " + currentSubTabsContainerHandler.getTabTitle());

        notificationMenuItem.setEnabled(false);
        popupMenu.add(notificationMenuItem);
        popupMenu.addSeparator();

        if (!currentSubTabsContainerHandler.isDotDotDotTab()) {
            JMenuItem pasteStyleMenu = new JMenuItem("Paste Style");
            if (sharedParameters.copiedTabFeaturesObjectStyle == null) {
                pasteStyleMenu.setEnabled(false);
            }
            pasteStyleMenu.addActionListener(e -> {
                if (sharedParameters.copiedTabFeaturesObjectStyle != null) {
                    currentSubTabsContainerHandler.updateByTabFeaturesObjectStyle(sharedParameters.copiedTabFeaturesObjectStyle, true);
                    sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
                    sharedParameters.printDebugMessage("Style pasted...");
                }
            });
            popupMenu.add(pasteStyleMenu);

            JMenuItem copyStyleMenu = new JMenuItem("Copy Style");
            copyStyleMenu.addActionListener(e -> {
                sharedParameters.copiedTabFeaturesObjectStyle = currentSubTabsContainerHandler.getTabFeaturesObjectStyle();
                sharedParameters.printDebugMessage("Style copied...");
            });
            popupMenu.add(copyStyleMenu);

            JMenuItem pasteStyleSearchTitleMenu = new JMenuItem("Find/Paste Style (Use RegEx in Title)");
            if (sharedParameters.copiedTabFeaturesObjectStyle == null) {
                pasteStyleSearchTitleMenu.setEnabled(false);
            }
            pasteStyleSearchTitleMenu.addActionListener(e -> {
                if (sharedParameters.copiedTabFeaturesObjectStyle != null) {
                    String titleKeyword = UIHelper.showPlainInputMessage("Enter a Regular Expression (case insensitive):", "Search in titles and replace their style", sharedParameters.searchedTabTitleForPasteStyle, sharedParameters.get_mainFrameUsingMontoya());
                    if (!titleKeyword.isEmpty()) {
                        if (Utilities.isValidRegExPattern(titleKeyword)) {
                            sharedParameters.searchedTabTitleForPasteStyle = titleKeyword;
                            ArrayList<SubTabsContainerHandler> subTabsContainerHandlers = sharedParameters.allSubTabContainerHandlers.get(currentSubTabsContainerHandler.currentToolTab);
                            for (SubTabsContainerHandler subTabsContainerHandlerItem : subTabsContainerHandlers) {
                                if (subTabsContainerHandlerItem.getVisible()) {
                                    String subTabTitle = subTabsContainerHandlerItem.getTabTitle();
                                    if (Pattern.compile(titleKeyword, Pattern.CASE_INSENSITIVE).matcher(subTabTitle).find()) {
                                        subTabsContainerHandlerItem.updateByTabFeaturesObjectStyle(sharedParameters.copiedTabFeaturesObjectStyle, true);
                                    }
                                }
                            }
                            sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler.currentToolTab);
                            sharedParameters.printDebugMessage("Style pasted in titles which matched: " + titleKeyword);
                        } else {
                            UIHelper.showWarningMessage("Regular expression was invalid.", sharedParameters.get_mainFrameUsingMontoya());
                            sharedParameters.printlnError("invalid regex: " + titleKeyword);
                        }
                    }

                }
            });
            popupMenu.add(pasteStyleSearchTitleMenu);

            JMenuItem pasteStyleForAllVisibleMenu = new JMenuItem("Paste Style For All Visible Tabs");
            if (sharedParameters.copiedTabFeaturesObjectStyle == null) {
                pasteStyleForAllVisibleMenu.setEnabled(false);
            }
            pasteStyleForAllVisibleMenu.addActionListener(e -> {
                int response = UIHelper.askConfirmMessage("Sharpener Extension: Changing all visible tabs' styles", "Are you sure you want to change all visible tab's style (you cannot undo this)?", new String[]{"Yes", "No"}, sharedParameters.get_mainFrameUsingMontoya());
                if (response == 0) {
                    if (sharedParameters.copiedTabFeaturesObjectStyle != null) {
                        ArrayList<SubTabsContainerHandler> subTabsContainerHandlers = sharedParameters.allSubTabContainerHandlers.get(currentSubTabsContainerHandler.currentToolTab);
                        for (SubTabsContainerHandler subTabsContainerHandlerItem : subTabsContainerHandlers) {
                            if (subTabsContainerHandlerItem.getVisible()) {
                                subTabsContainerHandlerItem.updateByTabFeaturesObjectStyle(sharedParameters.copiedTabFeaturesObjectStyle, true);
                            }
                        }
                        sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
                        sharedParameters.printDebugMessage("Style pasted...");
                    }
                }
            });
            popupMenu.add(pasteStyleForAllVisibleMenu);

            JMenuItem defaultProfile = new JMenuItem("Reset to Default");
            defaultProfile.addActionListener(e -> {
                currentSubTabsContainerHandler.setToDefault(true);
                sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
            });
            if (currentSubTabsContainerHandler.isDefault())
                defaultProfile.setEnabled(false);
            popupMenu.add(defaultProfile);

            JMenu profileMenu = new JMenu("Predefined Styles");
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "High - Confirmed", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, false, false, "#f71414", "high"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "High - Unconfirmed", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, false, false, "#f71414", "high-tbc"));

            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Medium - Confirmed", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, false, false, "#ff7e0d", "medium"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Medium - Unconfirmed", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, false, false, "#ff7e0d", "medium-tbc"));

            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Low - Confirmed", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, false, false, "#FAD400", "low"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Low - Unconfirmed", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, false, false, "#FAD400", "low-tbc"));

            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Info - Confirmed", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, true, false, "#0d9e1e", "info"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Info - Unconfirmed", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, true, false, "#0d9e1e", "info-tbc"));

            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Interesting 1", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, true, false, "#395EEA", "interesting"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Interesting 2", currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), true, true, false, "#D641CF", "interesting2"));

            /*
            // originals:
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "High - Confirmed", "Arial", 18, true, false, false, "#f71414", "high"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "High - Unconfirmed", "Arial", 18, true, false, false, "#f71414", "high-tbc"));

            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Medium - Confirmed", "Arial", 18, true, false, false, "#ff7e0d", "medium"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Medium - Unconfirmed", "Arial", 18, true, false, false, "#ff7e0d", "medium-tbc"));

            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Low - Confirmed", "Arial", 16, true, false, false, "#FAD400", "low"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Low - Unconfirmed", "Arial", 16, true, false, false, "#FAD400", "low-tbc"));

            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Info - Confirmed", "Arial", 16, true, true, false, "#0d9e1e", "info"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Info - Unconfirmed", "Arial", 16, true, true, false, "#0d9e1e", "info-tbc"));

            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Interesting 1", "Arial", 16, true, true, false, "#395EEA", "interesting"));
            profileMenu.add(predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, "Interesting 2", "Arial", 16, true, true, false, "#D641CF", "interesting2"));
            */
            profileMenu.add(predefinedStyleMenuByIcon(sharedParameters, currentSubTabsContainerHandler, "False Positive", "false-positive"));
            profileMenu.add(predefinedStyleMenuByIcon(sharedParameters, currentSubTabsContainerHandler, "Duplicate", "duplicate"));
            profileMenu.add(predefinedStyleMenuByIcon(sharedParameters, currentSubTabsContainerHandler, "Tick", "tick"));
            profileMenu.add(predefinedStyleMenuByIcon(sharedParameters, currentSubTabsContainerHandler, "Cross", "cross"));
            popupMenu.add(profileMenu);

            JMenu customStyleMenu = new JMenu("Custom Style");

            JMenu fontNameMenu = new JScrollMenu("Font Name");
            String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

            for (String font : fonts) {
                JCheckBoxMenuItem fontNameItem = new JCheckBoxMenuItem(font);
                fontNameItem.setSelected(font.equalsIgnoreCase(currentSubTabsContainerHandler.getFontName()));
                fontNameItem.addActionListener(e -> {
                    currentSubTabsContainerHandler.setFontName(font, true);
                    sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);

                });
                fontNameMenu.add(fontNameItem);
            }
            customStyleMenu.add(fontNameMenu);

            JMenu fontSizeMenu = new JMenu("Font Size");
            float minFontSize = 10, maxFontSize = 40;
            for (float fontSize = minFontSize; fontSize < maxFontSize; fontSize += 2) {
                JCheckBoxMenuItem sizeItem = new JCheckBoxMenuItem(fontSize + "");
                sizeItem.setSelected(currentSubTabsContainerHandler.getFontSize() == fontSize);
                float finalFontSize = fontSize;
                sizeItem.addActionListener(e -> {
                    currentSubTabsContainerHandler.setFontSize(finalFontSize, true);
                    sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
                });
                fontSizeMenu.add(sizeItem);
            }
            customStyleMenu.add(fontSizeMenu);

            JCheckBoxMenuItem boldMenu = new JCheckBoxMenuItem("Bold");
            boldMenu.setSelected(currentSubTabsContainerHandler.isBold());
            boldMenu.addActionListener(e -> {
                currentSubTabsContainerHandler.toggleBold(true);
                sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
            });
            customStyleMenu.add(boldMenu);

            JCheckBoxMenuItem italicMenu = new JCheckBoxMenuItem("Italic");
            italicMenu.setSelected(currentSubTabsContainerHandler.isItalic());
            italicMenu.addActionListener(e -> {
                currentSubTabsContainerHandler.toggleItalic(true);
                sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
            });
            customStyleMenu.add(italicMenu);


            // the icons are loaded once and cached, so opening the menu stays fast
            List<ResourceIconCache.NamedIcon> resourceIcons =
                    ResourceIconCache.getIcons(sharedParameters.extensionClass, SUB_TAB_ICON_FOLDER, SUB_TAB_ICON_MENU_WIDTH);
            if (resourceIcons.isEmpty()) {
                sharedParameters.printDebugMessage("No icon was found in resources");
            }

            JMenu changeTabIcon = new JScrollMenu("Icon");

            ButtonGroup subTabIconGroup = new ButtonGroup();

            JRadioButtonMenuItem noneIconImage = new JRadioButtonMenuItem("None");
            if (!currentSubTabsContainerHandler.hasIcon()) {
                noneIconImage.setSelected(true);
            }
            noneIconImage.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    currentSubTabsContainerHandler.removeIcon(false);
                    sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
                }
            });
            subTabIconGroup.add(noneIconImage);
            changeTabIcon.add(noneIconImage);

            for (ResourceIconCache.NamedIcon resourceIcon : resourceIcons) {
                if (resourceIcon == null)
                    continue;

                JRadioButtonMenuItem subTabIconImage = new JRadioButtonMenuItem(resourceIcon.fileName().replaceAll("\\..*$", ""));
                subTabIconImage.setIcon(resourceIcon.icon());
                String fileNameWithOutExt = Files.getNameWithoutExtension(resourceIcon.fileName());

                if (fileNameWithOutExt.equalsIgnoreCase(currentSubTabsContainerHandler.getIconString())) {
                    subTabIconImage.setSelected(true);
                }
                subTabIconImage.addActionListener((e) -> {
                    currentSubTabsContainerHandler.setIcon(fileNameWithOutExt, (int) currentSubTabsContainerHandler.getFontSize(), false);
                    sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
                });
                subTabIconGroup.add(subTabIconImage);
                changeTabIcon.add(subTabIconImage);
            }
            customStyleMenu.add(changeTabIcon);

            JMenuItem colorMenu = new JMenuItem("Set Foreground Color");
            colorMenu.addActionListener(e -> {
                JColorChooser colorChooser = new JColorChooser();
                // we only want to keep the Swatches panel
                AbstractColorChooserPanel[] panels = colorChooser.getChooserPanels();
                for (AbstractColorChooserPanel p : panels) {
                    String displayName = p.getDisplayName();
                    switch (displayName) {
                        //case "RGB":
                        case "HSL":
                        case "HSV":
                        case "CMYK":
                            colorChooser.removeChooserPanel(p);
                            break;
                    }
                }

                colorChooser.setColor(currentSubTabsContainerHandler.getColor());
                JDialog dialog = JColorChooser.createDialog(
                        sharedParameters.get_mainFrameUsingMontoya(),
                        "Choose a Color",
                        true,
                        colorChooser,
                        null,
                        null);
                dialog.setVisible(true);
                if (colorChooser.getColor() != null) {
                    currentSubTabsContainerHandler.setColor(colorChooser.getColor(), true);
                    sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
                }
            });
            customStyleMenu.add(colorMenu);
            popupMenu.add(customStyleMenu);

            popupMenu.addSeparator();
        }

        JMenu searchAndJumpMenu = new JMenu("Find Title (Use RegEx)");

        JMenuItem searchAndJumpDefineRegExMenu = new JMenuItem("Search by RegEx (case insensitive)" + ShortcutMappings.menuHint(sharedParameters, "FindTabs"));

        searchAndJumpDefineRegExMenu.addActionListener(e -> {
            defineRegExPopupForSearchAndJump(sharedParameters, currentSubTabsContainerHandler);
        });
        searchAndJumpMenu.add(searchAndJumpDefineRegExMenu);

        JMenuItem jumpToNextTabByTitleMenu = new JMenuItem("Next" + ShortcutMappings.menuHint(sharedParameters, "NextFind"));
        if (sharedParameters.searchedTabTitleForJumpToTab.isEmpty()) {
            jumpToNextTabByTitleMenu.setEnabled(false);
        } else {
            jumpToNextTabByTitleMenu.setToolTipText("Search for: " + sharedParameters.searchedTabTitleForJumpToTab);
        }

        jumpToNextTabByTitleMenu.addActionListener(e -> {
            searchInTabTitlesAndJump(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem, true);
        });
        searchAndJumpMenu.add(jumpToNextTabByTitleMenu);

        JMenuItem jumpToPreviousTabByTitleMenu = new JMenuItem("Previous" + ShortcutMappings.menuHint(sharedParameters, "PreviousFind"));
        if (sharedParameters.searchedTabTitleForJumpToTab.isEmpty()) {
            jumpToPreviousTabByTitleMenu.setEnabled(false);
        } else {
            jumpToPreviousTabByTitleMenu.setToolTipText("Search for: " + sharedParameters.searchedTabTitleForJumpToTab);
        }

        jumpToPreviousTabByTitleMenu.addActionListener(e -> {
            searchInTabTitlesAndJump(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem, false);
        });

        searchAndJumpMenu.add(jumpToPreviousTabByTitleMenu);

        if (sharedParameters.searchedTabTitleForJumpToTab.isEmpty()) {
            searchAndJumpMenu.setText("Find Title (Click > Use RegEx)");

            searchAndJumpMenu.addMouseListener(new MouseAdapterExtensionHandler(mouseEvent -> {
                if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                    searchAndJumpDefineRegExMenu.doClick();
                    popupMenu.setVisible(false);

                }
            }, MouseEvent.MOUSE_CLICKED));
        } else {
            // we want to rename searchAndJumpMenu, so it shows what would happen when it is clicked!
            searchAndJumpMenu.setText("Find Title (Click > Next, Right-Click > Prev)");

            searchAndJumpMenu.addMouseListener(new MouseAdapterExtensionHandler(mouseEvent -> {
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    jumpToPreviousTabByTitleMenu.doClick();
                } else {
                    jumpToNextTabByTitleMenu.doClick();
                }
            }, MouseEvent.MOUSE_CLICKED));
        }

        popupMenu.add(searchAndJumpMenu);

        JMenuItem copyTitleMenu = new JMenuItem("Copy Title" + ShortcutMappings.menuHint(sharedParameters, "CopyTitle"));
        copyTitleMenu.addActionListener(e -> {
            copyTitle(sharedParameters, currentSubTabsContainerHandler);
        });
        popupMenu.add(copyTitleMenu);

        JMenuItem pasteTitleMenu = new JMenuItem("Paste Title" + ShortcutMappings.menuHint(sharedParameters, "PasteTitle"));

        // The item starts disabled and is enabled once the clipboard value arrives,
        // usually before the menu is even visible. The clipboard must not be read on
        // the EDT here: the owner of the clipboard is another process and a slow or
        // stuck owner would freeze the whole Burp UI during menu construction.
        pasteTitleMenu.setEnabled(false);
        readClipboardTitleAsync(sharedParameters, clipboardTitle -> {
            if (!clipboardTitle.isBlank()) {
                pasteTitleMenu.setEnabled(true);
                pasteTitleMenu.setToolTipText("Clipboard value: " + StringUtils.abbreviate(clipboardTitle, 100));
            }
        });

        pasteTitleMenu.addActionListener(e -> {
            pasteTitle(sharedParameters, currentSubTabsContainerHandler);
        });
        popupMenu.add(pasteTitleMenu);

        JMenuItem renameTitleMenu = new JMenuItem("Rename Title" + ShortcutMappings.menuHint(sharedParameters, "RenameTitle"));
        renameTitleMenu.addActionListener(e -> {
            renameTitle(sharedParameters, currentSubTabsContainerHandler);
        });
        popupMenu.add(renameTitleMenu);


        JMenuItem matchReplaceTitleMenu = new JMenuItem("Match/Replace Titles (Use RegEx)");
        matchReplaceTitleMenu.addActionListener(e -> {
            String[] matchReplaceResult = UIHelper.showPlainInputMessages(new String[]{"Find what (start it with `(?i)` for case insensitive RegEx):", "Replace with:"}, "Title Match and Replace (RegEx)", new String[]{sharedParameters.matchReplaceTitle_RegEx, sharedParameters.matchReplaceTitle_ReplaceWith}, sharedParameters.get_mainFrameUsingMontoya());
            sharedParameters.matchReplaceTitle_RegEx = (matchReplaceResult[0] != null) ? matchReplaceResult[0] : "";
            sharedParameters.matchReplaceTitle_ReplaceWith = (matchReplaceResult[1] != null) ? matchReplaceResult[1] : "";
            if (!sharedParameters.matchReplaceTitle_RegEx.isEmpty()) {
                if (Utilities.isValidRegExPattern(sharedParameters.matchReplaceTitle_RegEx)) {
                    ArrayList<SubTabsContainerHandler> subTabsContainerHandlers = sharedParameters.allSubTabContainerHandlers.get(currentSubTabsContainerHandler.currentToolTab);
                    for (SubTabsContainerHandler subTabsContainerHandlerItem : subTabsContainerHandlers) {
                        if (subTabsContainerHandlerItem.getVisible()) {
                            String subTabTitle = subTabsContainerHandlerItem.getTabTitle();
                            if (Pattern.compile(sharedParameters.matchReplaceTitle_RegEx).matcher(subTabTitle).find()) {
                                subTabsContainerHandlerItem.setTabTitle(subTabsContainerHandlerItem.getTabTitle().replaceAll(sharedParameters.matchReplaceTitle_RegEx, sharedParameters.matchReplaceTitle_ReplaceWith), true);
                            }
                        }
                    }
                    sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler.currentToolTab);
                    sharedParameters.printDebugMessage("Match and replace titles finished. -RegEx: " + sharedParameters.matchReplaceTitle_RegEx + " -Replace with: " + sharedParameters.matchReplaceTitle_ReplaceWith);
                } else {
                    UIHelper.showWarningMessage("Regular expression was invalid.", sharedParameters.get_mainFrameUsingMontoya());
                    sharedParameters.printlnError("invalid regex: " + sharedParameters.matchReplaceTitle_RegEx);
                }
            }
        });
        popupMenu.add(matchReplaceTitleMenu);

        JMenu previousTitlesMenu = new JMenu("Previous Titles");

        JMenu previousTitlesMenuSet = new JMenu("Set");
        JMenu previousTitlesMenuCopy = new JMenu("Copy");
        JMenuItem previousTitlesMenuClearHistory = new JMenuItem("Clear History");

        if (currentSubTabsContainerHandler.getTitleHistory().length <= 1) {
            previousTitlesMenu.setEnabled(false);
            previousTitlesMenuSet.setEnabled(false);
            previousTitlesMenuCopy.setEnabled(false);
            previousTitlesMenuClearHistory.setEnabled(false);
        } else {
            String[] uniqueInvertedTitleHistoryArray = currentSubTabsContainerHandler.getTitleHistory();

            for (String tempPrevTitle : uniqueInvertedTitleHistoryArray) {
                if (!tempPrevTitle.equalsIgnoreCase(currentSubTabsContainerHandler.getTabTitle())) {
                    JMenuItem previousTitleMenuSet = new JMenuItem(new AbstractAction(tempPrevTitle) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            currentSubTabsContainerHandler.setTabTitle(tempPrevTitle, true);
                            sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
                            sharedParameters.printDebugMessage("Previous title has been set.");
                        }
                    });
                    previousTitlesMenuSet.add(previousTitleMenuSet);

                    JMenuItem previousTitleMenuCopy = new JMenuItem(new AbstractAction(tempPrevTitle) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Toolkit.getDefaultToolkit()
                                    .getSystemClipboard()
                                    .setContents(
                                            new StringSelection(tempPrevTitle),
                                            null
                                    );
                            sharedParameters.printDebugMessage("A previous title has been copied.");
                        }
                    });
                    previousTitlesMenuCopy.add(previousTitleMenuCopy);
                }
            }

            previousTitlesMenuClearHistory.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    currentSubTabsContainerHandler.setTitleHistory(null);
                    sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
                    sharedParameters.printDebugMessage("Previous titles have been cleared.");
                }
            });
        }
        previousTitlesMenu.add(previousTitlesMenuSet);
        previousTitlesMenu.add(previousTitlesMenuCopy);
        previousTitlesMenu.add(previousTitlesMenuClearHistory);


        popupMenu.add(previousTitlesMenu);


        popupMenu.addSeparator();

        JMenu jumpMenu = new JMenu("Jump To (Click > Next, Right-Click > Prev)");
        JMenuItem jumpToFirstTabMenu = new JMenuItem("First Tab" + ShortcutMappings.menuHint(sharedParameters, "FirstTab"));

        jumpToFirstTabMenu.addActionListener(e -> {
            jumpToFirstTab(sharedParameters, currentSubTabsContainerHandler);
        });


        jumpMenu.add(jumpToFirstTabMenu);

        JMenuItem jumpToLastTabMenu = new JMenuItem("Last Tab" + ShortcutMappings.menuHint(sharedParameters, "LastTab"));

        jumpToLastTabMenu.addActionListener(e -> {
            jumpToLastTab(sharedParameters, currentSubTabsContainerHandler);
        });

        jumpMenu.add(jumpToLastTabMenu);

        JMenuItem jumpToPreviousTabMenu = new JMenuItem("Previous Tab" + ShortcutMappings.menuHint(sharedParameters, "PreviousTab"));

        jumpToPreviousTabMenu.addActionListener(e -> {
            jumpToPreviousTab(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem);
        });

        jumpMenu.add(jumpToPreviousTabMenu);

        JMenuItem jumpToNextTabMenu = new JMenuItem("Next Tab" + ShortcutMappings.menuHint(sharedParameters, "NextTab"));
        jumpToNextTabMenu.addActionListener(e -> {
            jumpToNextTab(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem);
        });
        jumpMenu.add(jumpToNextTabMenu);

        JMenuItem jumpToPreviouslySelectedTabMenu = new JMenuItem("Back" + ShortcutMappings.menuHint(sharedParameters, "PreviouslySelectedTab"));
        if (sharedParameters.subTabPreviouslySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).size() <= 0)
            jumpToPreviouslySelectedTabMenu.setEnabled(false);
        jumpToPreviouslySelectedTabMenu.addActionListener(e -> {
            jumpToPreviouslySelectedTab(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem);
        });
        jumpMenu.add(jumpToPreviouslySelectedTabMenu);

        JMenuItem jumpToNextlySelectedTabMenu = new JMenuItem("Forward" + ShortcutMappings.menuHint(sharedParameters, "NextlySelectedTab"));
        if (sharedParameters.subTabNextlySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).size() <= 0)
            jumpToNextlySelectedTabMenu.setEnabled(false);
        jumpToNextlySelectedTabMenu.addActionListener(e -> {
            jumpToNextlySelectedTab(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem);
        });
        jumpMenu.add(jumpToNextlySelectedTabMenu);


        jumpMenu.addMouseListener(new MouseAdapterExtensionHandler(mouseEvent -> {
            if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                jumpToPreviousTabMenu.doClick();
            } else {
                jumpToNextTabMenu.doClick();
            }
        }, MouseEvent.MOUSE_CLICKED));

        popupMenu.add(jumpMenu);

        JMenu tabScreenshotMenu = new JMenu("Capture Screenshot");
        JMenuItem saveScreenshotToClipboardMenu = new JMenuItem("Clipboard");
        saveScreenshotToClipboardMenu.addActionListener(e -> {

            Rectangle componentRect = currentSubTabsContainerHandler.parentTabbedPane.getSelectedComponent().getBounds();
            BufferedImage bufferedImage = new BufferedImage(componentRect.width, componentRect.height, BufferedImage.TYPE_INT_RGB);
            currentSubTabsContainerHandler.parentTabbedPane.getSelectedComponent().paint(bufferedImage.getGraphics());
            ImageHelper.setClipboard(bufferedImage);
        });
        tabScreenshotMenu.add(saveScreenshotToClipboardMenu);

        JMenuItem saveScreenshotToFileMenu = new JMenuItem("File");
        saveScreenshotToFileMenu.addActionListener(e -> {
            Rectangle componentRect = currentSubTabsContainerHandler.parentTabbedPane.getSelectedComponent().getBounds();
            BufferedImage bufferedImage = new BufferedImage(componentRect.width, componentRect.height, BufferedImage.TYPE_INT_ARGB);
            currentSubTabsContainerHandler.parentTabbedPane.getSelectedComponent().paint(bufferedImage.getGraphics());

            String saveLocation = UIHelper.showDirectorySaveDialog(sharedParameters.allSettings.subTabsSettings.lastSavedImageLocation, sharedParameters.get_mainFrameUsingMontoya());

            if (!saveLocation.isEmpty()) {
                sharedParameters.allSettings.subTabsSettings.lastSavedImageLocation = saveLocation;
                String strDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String imageFileLocation = saveLocation + "/" + currentSubTabsContainerHandler.getTabTitle().replaceAll("[^a-zA-Z0-9-_.]", "_") + "_" + strDate + ".png";

                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, "png", os);
                    try (OutputStream outputStream = new FileOutputStream(imageFileLocation)) {
                        os.writeTo(outputStream);
                    }
                } catch (Exception err) {
                    sharedParameters.printlnError("Image file could not be saved: " + imageFileLocation);
                    sharedParameters.printDebugMessage(err.getMessage());
                }

                File imageFile = new File(imageFileLocation);
                if (imageFile.exists()) {
                    sharedParameters.printlnOutput("Image file saved successfully: " + imageFileLocation);
                } else {
                    sharedParameters.printlnError("Image file could not be saved: " + imageFileLocation);
                    UIHelper.showWarningMessage("Image file could not be saved: " + imageFileLocation, sharedParameters.get_mainFrameUsingMontoya());
                }

            }
        });
        tabScreenshotMenu.add(saveScreenshotToFileMenu);
        popupMenu.add(tabScreenshotMenu);

        if (!sharedParameters.isTabGroupSupportedByDefault) {
            JMenuItem jumpToAddTabMenu = new JMenuItem("Add an Empty New Tab");

            jumpToAddTabMenu.addActionListener(actionEvent -> {

                Container dotdotdotTabContainer = (Container) currentSubTabsContainerHandler.parentTabbedPane.getTabComponentAt(currentSubTabsContainerHandler.parentTabbedPane.getTabCount() - 1);

                // this is a hack to get the Y location of the ... tab!
                int x = dotdotdotTabContainer.getLocationOnScreen().x + dotdotdotTabContainer.getWidth() / 2;
                int burp_x = dotdotdotTabContainer.getParent().getLocationOnScreen().x + dotdotdotTabContainer.getParent().getWidth() - dotdotdotTabContainer.getWidth() / 2;
                if (x > burp_x) {
                    x = burp_x;
                }

                int y = dotdotdotTabContainer.getLocationOnScreen().y + dotdotdotTabContainer.getHeight() / 2;
                int burp_y = dotdotdotTabContainer.getParent().getLocationOnScreen().y + dotdotdotTabContainer.getParent().getHeight() - dotdotdotTabContainer.getHeight() / 2;
                if (y > burp_y || y < burp_y - dotdotdotTabContainer.getHeight()) {
                    y = burp_y;
                }

                try {
                    Robot robot = new Robot();
                    robot.mouseMove(x, y);
                } catch (Exception errRobot) {
                    sharedParameters.printlnError("Could not change mouse location: " + errRobot.getMessage());
                }

                jumpToPreviouslySelectedTab(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem);

                jumpToTabIndex(sharedParameters, currentSubTabsContainerHandler, currentSubTabsContainerHandler.parentTabbedPane.getTabCount() - 1);
            });

            popupMenu.add(jumpToAddTabMenu);
        }

        popupMenu.addSeparator();

        BurpUITools.MainTabs tool = currentSubTabsContainerHandler.currentToolTab;

        if (sharedParameters.getAccessibleSubTabSupportedTabs().contains(tool)) {
            JCheckBoxMenuItem toolSubTabPaneMouseWheelScroll = new JCheckBoxMenuItem("Activate Mouse Wheel: MW > Scroll, MW+Ctrl > Resize");
            if (sharedParameters.preferences.safeGetBooleanSetting("mouseWheelToScroll_" + tool)) {
                toolSubTabPaneMouseWheelScroll.setSelected(true);
            }

            toolSubTabPaneMouseWheelScroll.addActionListener((e) -> {
                if (sharedParameters.preferences.safeGetBooleanSetting("mouseWheelToScroll_" + tool)) {
                    SubTabsActions.removeMouseWheelFromJTabbedPane(sharedParameters, tool);
                    sharedParameters.preferences.safeSetSetting("mouseWheelToScroll_" + tool, false, Preferences.Visibility.PROJECT);
                } else {
                    SubTabsActions.addMouseWheelToJTabbedPane(sharedParameters, tool, sharedParameters.isTabGroupSupportedByDefault);
                    sharedParameters.preferences.safeSetSetting("mouseWheelToScroll_" + tool, true, Preferences.Visibility.PROJECT);
                }
            });

            popupMenu.add(toolSubTabPaneMouseWheelScroll);
        }

        popupMenu.addSeparator();

        JMenuItem keyboardShortcutsMenu = new JMenuItem("Keyboard Shortcuts");
        keyboardShortcutsMenu.setToolTipText("Shows all Sharpener shortcuts and allows changing them");
        keyboardShortcutsMenu.addActionListener(e -> ShortcutsDialog.show(sharedParameters));
        popupMenu.add(keyboardShortcutsMenu);

        return popupMenu;
    }

    private static JMenuItem predefinedStyleMenuByIcon(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, String text, String iconString) {
        return predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, text, currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), currentSubTabsContainerHandler.isBold(), currentSubTabsContainerHandler.isItalic(), currentSubTabsContainerHandler.getVisibleCloseButton(), currentSubTabsContainerHandler.getColorCode(), iconString);
    }

    private static JMenuItem predefinedStyleMenu(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, String text, String fontName, int fontSize, boolean isBold, boolean isItalic, boolean isCloseButtonVisible, String colorCode, String iconString) {
        if (currentSubTabsContainerHandler == null)
            return null;

        JMenuItem profile = new JMenuItem(text);
        int style = profile.getFont().getStyle();

        if (isBold)
            style ^= Font.BOLD;

        if (isItalic)
            style ^= Font.ITALIC;

        profile.setFont(new Font(fontName, style, fontSize));
        profile.setForeground(Color.decode(colorCode));
        profile.setIcon(new ImageIcon(Objects.requireNonNull(ImageHelper.scaleImageToWidth(ImageHelper.loadImageResource(sharedParameters.extensionClass, "subtabicons/" + iconString + ".png"), fontSize))));
        profile.addActionListener(e -> {
            TabFeaturesObjectStyle tabFeaturesObjectStyle = new TabFeaturesObjectStyle(text, fontName, fontSize, isBold, isItalic, isCloseButtonVisible, Color.decode(colorCode), iconString, fontSize);
            currentSubTabsContainerHandler.updateByTabFeaturesObjectStyle(tabFeaturesObjectStyle, true);
            sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
        });
        return profile;
    }

    private static JMenuItem predefinedStyleMenuWithNoFontChange(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, String text, boolean isCloseButtonVisible, String colorCode, String iconString) {
        return predefinedStyleMenu(sharedParameters, currentSubTabsContainerHandler, text, currentSubTabsContainerHandler.getFontName(), (int) currentSubTabsContainerHandler.getFontSize(), currentSubTabsContainerHandler.isBold(), currentSubTabsContainerHandler.isItalic(), isCloseButtonVisible, colorCode, iconString);
    }

    public static boolean changeToolTabbedPaneUI_safe(ExtensionSharedParameters sharedParameters, BurpUITools.MainTabs currentToolTab, boolean shouldOriginalBeSet, int counter) {
        boolean result = true;
        try {
            // should have already been loaded but just in case something has changed
            // hopefully it has not been tainted already!
            var currentToolTabbedPane = sharedParameters.get_toolTabbedPane(currentToolTab);
            if (currentToolTabbedPane == null) {
                sharedParameters.printDebugMessage("Error in getting the current tool tabs: " + currentToolTab);
                return false;
            }

            if (sharedParameters.originalSubTabbedPaneUI.get(currentToolTab) == null) {
                sharedParameters.originalSubTabbedPaneUI.put(currentToolTab,
                        currentToolTabbedPane.getUI());
            }

            boolean isMinimizeTabSize = sharedParameters.preferences.safeGetBooleanSetting("minimizeSize_" + currentToolTab);
            boolean isFixedTabPosition = (sharedParameters.preferences.safeGetBooleanSetting("isTabFixedPosition_" + currentToolTab));
            boolean isFiltered = sharedParameters.isFiltered(currentToolTab);

            boolean isOriginal = shouldOriginalBeSet && !isMinimizeTabSize && !isFiltered && isFixedTabPosition;


            if (isOriginal || sharedParameters.isTabGroupSupportedByDefault) {
                currentToolTabbedPane.updateUI();
            } else {
                currentToolTabbedPane.setUI(SubTabsCustomTabbedPaneUI.getUI(sharedParameters, currentToolTab));
            }

            currentToolTabbedPane.revalidate();
            currentToolTabbedPane.repaint();
            sharedParameters.delayedTasks.schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            if (sharedParameters.isUnloaded())
                                return;
                            sharedParameters.allSettings.subTabsSettings.updateSubTabsUI(currentToolTab);
                        }
                    },
                    3000 // 3 seconds-delay to ensure all has been settled!
            );
        } catch (Exception e) {
            result = false;
        }

        return result;
    }


    public static void changeToolTabbedPaneUI_safe(ExtensionSharedParameters sharedParameters, BurpUITools.MainTabs currentToolTab, boolean shouldOriginalBeSet) {
        changeToolTabbedPaneUI_safe_withRetry(sharedParameters, currentToolTab, shouldOriginalBeSet, 0);
    }

    public static void changeToolTabbedPaneUI_safe(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, boolean shouldOriginalBeSet) {
        changeToolTabbedPaneUI_safe_withRetry(sharedParameters, currentSubTabsContainerHandler.currentToolTab, shouldOriginalBeSet, 0);
    }

    // Tries the UI change up to 3 times, waiting 1 second before each attempt.
    // A javax.swing.Timer is used instead of Thread.sleep so the EDT is never blocked,
    // which used to make every load and reload feel sluggish. The 1 second wait before the
    // first attempt is kept so Burp can finish installing its own tab UI first, otherwise
    // its BurpTabbedPaneUI can throw a NullPointerException while laying out the tabs.
    private static void changeToolTabbedPaneUI_safe_withRetry(ExtensionSharedParameters sharedParameters, BurpUITools.MainTabs currentToolTab, boolean shouldOriginalBeSet, int attempt) {
        if (sharedParameters.isUnloaded() || attempt >= 3)
            return;
        // the timer fires on the EDT, so the setUI call below runs on the EDT without blocking it
        javax.swing.Timer settleTimer = new javax.swing.Timer(1000, e -> {
            if (sharedParameters.isUnloaded())
                return;
            // sometimes we have errors when using SetUI - we use this error catching and retry mechanism to hopefully overcome this!
            sharedParameters.printDebugMessage("Try number " + attempt + " to update the UI");
            boolean isSuccessful = changeToolTabbedPaneUI_safe(sharedParameters, currentToolTab, shouldOriginalBeSet, attempt);
            if (!isSuccessful) {
                changeToolTabbedPaneUI_safe_withRetry(sharedParameters, currentToolTab, shouldOriginalBeSet, attempt + 1);
            }
        });
        settleTimer.setRepeats(false);
        settleTimer.start();
    }

    public static void setTabTitleFilter(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler == null)
            return;

        // filterOperationMode -->
        //operationMode=0 -> use RegEx
        //operationMode=1 -> Custom style only
        //operationMode=2 -> Custom style or not numerical
        //operationMode=3 -> Websocket tabs

        if (!sharedParameters.titleFilterRegEx.isBlank() || sharedParameters.filterOperationMode.get(currentSubTabsContainerHandler.currentToolTab) > 0) {
            for (SubTabsContainerHandler subTabsContainerHandlerItem : sharedParameters.allSubTabContainerHandlers.get(currentSubTabsContainerHandler.currentToolTab)) {
                String subTabTitle = subTabsContainerHandlerItem.getTabTitle();

                // if it is not negative (default), then if we have a match, we have a winner so default is false and vice versa
                // the exception is for mode 2, as if we have a match for numbers then we don't like it!
                boolean interestingItemUsingRegEx = sharedParameters.isTitleFilterNegative;

                if (!sharedParameters.titleFilterRegEx.isBlank()) {
                    // RegEx Matched
                    interestingItemUsingRegEx = Pattern.compile(sharedParameters.titleFilterRegEx, Pattern.CASE_INSENSITIVE).matcher(subTabTitle).find();
                }

                if (sharedParameters.filterOperationMode.get(currentSubTabsContainerHandler.currentToolTab) == 2) {
                    //  in Custom style or not numerical, we need to exclude all numbers as RegEx has been set to number only, we need to make it negative again!
                    interestingItemUsingRegEx = !interestingItemUsingRegEx;
                }

                if (sharedParameters.isTitleFilterNegative) {
                    // in negative state, anything positive will be negative at this point!
                    interestingItemUsingRegEx = !interestingItemUsingRegEx;
                }

                boolean interestingItemUsingStyle = sharedParameters.isTitleFilterNegative;
                if ((sharedParameters.filterOperationMode.get(currentSubTabsContainerHandler.currentToolTab) == 1 ||
                        sharedParameters.filterOperationMode.get(currentSubTabsContainerHandler.currentToolTab) == 2)) {
                    // Checking Custom style only when we do not think it is an interesting item by this point
                    interestingItemUsingStyle = !subTabsContainerHandlerItem.isDefault();

                    if (sharedParameters.isTitleFilterNegative) {
                        // in negative state, anything positive will be negative at this point!
                        interestingItemUsingStyle = !interestingItemUsingStyle;
                    }
                }

                boolean isItFinallyInteresting = false;
                if (sharedParameters.filterOperationMode.get(currentSubTabsContainerHandler.currentToolTab) == 2) {
                    // mode 2
                    isItFinallyInteresting = interestingItemUsingStyle || interestingItemUsingRegEx;
                } else if (sharedParameters.filterOperationMode.get(currentSubTabsContainerHandler.currentToolTab) == 1) {
                    // mode 1
                    isItFinallyInteresting = interestingItemUsingStyle;
                } else if (sharedParameters.filterOperationMode.get(currentSubTabsContainerHandler.currentToolTab) == 0) {
                    // mode 0
                    isItFinallyInteresting = interestingItemUsingRegEx;
                } else if (sharedParameters.filterOperationMode.get(currentSubTabsContainerHandler.currentToolTab) == 3) {
                    // mode 3
                    isItFinallyInteresting = subTabsContainerHandlerItem.isWebSocketTab() ^ sharedParameters.isTitleFilterNegative;
                }

                // if it is not an interesting item, we need to hide it!
                subTabsContainerHandlerItem.setVisible(isItFinallyInteresting);
            }

            // now we need to change the UI, so it will return 0 for width of the filtered tabs
            changingTabbedPaneUiToHideTabs(sharedParameters, currentSubTabsContainerHandler);
        }
    }

    private static void changingTabbedPaneUiToHideTabs(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler == null)
            return;

        var currentToolTabbedPane = sharedParameters.get_toolTabbedPane(currentSubTabsContainerHandler.currentToolTab);
        if (currentToolTabbedPane == null)
            return;

        if (sharedParameters.isFiltered(currentSubTabsContainerHandler.currentToolTab)) {
            sharedParameters.printDebugMessage("Changing UI so it can hide tabs");
            if (sharedParameters.originalSubTabbedPaneUI.get(currentSubTabsContainerHandler.currentToolTab) == null)
                sharedParameters.originalSubTabbedPaneUI.put(currentSubTabsContainerHandler.currentToolTab,
                        currentToolTabbedPane.getUI());

            changeToolTabbedPaneUI_safe(sharedParameters, currentSubTabsContainerHandler, false);
        } else {
            changeToolTabbedPaneUI_safe(sharedParameters, currentSubTabsContainerHandler, true);
            sharedParameters.printDebugMessage("Removing the filter");
        }
    }

    public static void showAllTabTitles(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler == null)
            return;

        if (sharedParameters.isFiltered(currentSubTabsContainerHandler.currentToolTab)) {
            for (SubTabsContainerHandler subTabsContainerHandlerItem : sharedParameters.allSubTabContainerHandlers.get(currentSubTabsContainerHandler.currentToolTab)) {
                subTabsContainerHandlerItem.setVisible(true);
            }
            changeToolTabbedPaneUI_safe(sharedParameters, currentSubTabsContainerHandler, true);
        }
    }

    public static void toggleCurrentTabVisibility(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler == null)
            return;

        var currentToolTabbedPane = sharedParameters.get_toolTabbedPane(currentSubTabsContainerHandler.currentToolTab);
        if (currentToolTabbedPane == null)
            return;

        currentSubTabsContainerHandler.setVisible(!currentSubTabsContainerHandler.getVisible());

        currentToolTabbedPane.revalidate();
        currentToolTabbedPane.repaint();
        sharedParameters.delayedTasks.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if (sharedParameters.isUnloaded())
                            return;
                        sharedParameters.allSettings.subTabsSettings.updateSubTabsUI(currentSubTabsContainerHandler.currentToolTab);
                    }
                },
                2000 // 2 seconds-delay to ensure all has been settled!
        );

        // now we need to change the UI, so it will return 0 for width of the filtered tabs
        changingTabbedPaneUiToHideTabs(sharedParameters, currentSubTabsContainerHandler);
    }

    public static SubTabsContainerHandler getSubTabContainerHandlerFromEvent(ExtensionSharedParameters sharedParameters, AWTEvent event) {
        SubTabsContainerHandler subTabsContainerHandler = null;
        if (event.getSource() instanceof Component) {
            JTabbedPane tabbedPane = (JTabbedPane) UIWalker.findUIObjectInParentComponents((Component) event.getSource(), 4, new UiSpecObject(JTabbedPane.class));
            if (tabbedPane != null) {
                int currentSelection = tabbedPane.getSelectedIndex();
                subTabsContainerHandler = getSubTabContainerHandlerFromSharedParameters(sharedParameters, tabbedPane, currentSelection);
            }
        }
        return subTabsContainerHandler;
    }

    public static SubTabsContainerHandler getSubTabContainerHandlerFromSharedParameters(ExtensionSharedParameters sharedParameters, JTabbedPane tabbedPane, int currentIndex) {
        SubTabsContainerHandler subTabsContainerHandler = null;

        SubTabsContainerHandler tempSubTabsContainerHandler = new SubTabsContainerHandler(sharedParameters, tabbedPane, currentIndex, true);
        BurpUITools.MainTabs currentToolTab = tempSubTabsContainerHandler.currentToolTab;

        ArrayList<SubTabsContainerHandler> subTabsContainerHandlers = sharedParameters.allSubTabContainerHandlers.get(currentToolTab);

        if (subTabsContainerHandlers != null) {
            int sharedParamIndex = subTabsContainerHandlers.indexOf(tempSubTabsContainerHandler);
            if (sharedParamIndex >= 0)
                subTabsContainerHandler = subTabsContainerHandlers.get(sharedParamIndex);
        }

        if (subTabsContainerHandler == null && tempSubTabsContainerHandler.isValid()
                && sharedParameters.allSettings != null && sharedParameters.allSettings.subTabsSettings != null) {
            // the tab exists but it was missed by the tab change listener,
            // a delayed reload detects it without needing a drag and drop
            sharedParameters.allSettings.subTabsSettings.scheduleTabRescan(currentToolTab);
        }

        return subTabsContainerHandler;
    }

    public static void showPopupMenu(ExtensionSharedParameters sharedParameters, AWTEvent event) {
        showPopupMenu(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event), null);
    }

    public static void showPopupMenu(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, AWTEvent event) {
        if (currentSubTabsContainerHandler == null)
            return;

        var currentToolTabbedPane = sharedParameters.get_toolTabbedPane(currentSubTabsContainerHandler.currentToolTab);
        if (currentToolTabbedPane == null)
            return;

        // creating popup menu
        JPopupMenu popupMenu = createPopupMenu(sharedParameters, currentSubTabsContainerHandler);
        int x;
        int y;
        if (currentToolTabbedPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT && event instanceof MouseEvent) {
            x = ((MouseEvent) event).getX();
            y = ((MouseEvent) event).getY() + currentToolTabbedPane.getTabComponentAt(currentSubTabsContainerHandler.getTabIndex()).getHeight() / 2;
        } else {
            x = currentToolTabbedPane.getTabComponentAt(currentSubTabsContainerHandler.getTabIndex()).getX();
            y = currentToolTabbedPane.getTabComponentAt(currentSubTabsContainerHandler.getTabIndex()).getY() + currentToolTabbedPane.getTabComponentAt(currentSubTabsContainerHandler.getTabIndex()).getHeight();
        }
        // showing popup menu
        popupMenu.show(currentToolTabbedPane, x, y);
    }

    public static void defineRegExPopupForSearchAndJump(ExtensionSharedParameters sharedParameters, AWTEvent event) {
        defineRegExPopupForSearchAndJump(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event));
    }

    public static void defineRegExPopupForSearchAndJump(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler != null)
            defineRegExPopupForSearchAndJump(sharedParameters, currentSubTabsContainerHandler.currentToolTab);
    }

    public static void defineRegExPopupForSearchAndJump(ExtensionSharedParameters sharedParameters, BurpUITools.MainTabs currentToolTab) {
        String titleKeyword = UIHelper.showPlainInputMessage("Enter a Regular Expression:", "Search in titles and jump to tab", sharedParameters.searchedTabTitleForJumpToTab, sharedParameters.get_mainFrameUsingMontoya());
        if (!titleKeyword.isEmpty()) {
            boolean result = false;
            if (Utilities.isValidRegExPattern(titleKeyword)) {
                sharedParameters.searchedTabTitleForJumpToTab = titleKeyword;
                ArrayList<SubTabsContainerHandler> subTabsContainerHandlers = sharedParameters.allSubTabContainerHandlers.get(currentToolTab);
                for (SubTabsContainerHandler subTabsContainerHandlerItem : subTabsContainerHandlers) {
                    if (subTabsContainerHandlerItem.getVisible()) {
                        String subTabTitle = subTabsContainerHandlerItem.getTabTitle();
                        if (Pattern.compile(titleKeyword, Pattern.CASE_INSENSITIVE).matcher(subTabTitle).find()) {
                            jumpToTabIndex(sharedParameters, subTabsContainerHandlerItem, subTabsContainerHandlerItem.getTabIndex());
                            result = true;
                            break;
                        }
                    }
                }
                if (result) {
                    sharedParameters.printDebugMessage("Jumped to first title which matched: " + titleKeyword);
                } else {
                    sharedParameters.printDebugMessage("No title matched: " + titleKeyword);
                }

            } else {
                UIHelper.showWarningMessage("Regular expression was invalid.", sharedParameters.get_mainFrameUsingMontoya());
                sharedParameters.printlnError("invalid regex: " + titleKeyword);
            }
        }
    }

    public static void searchInTabTitlesAndJump(ExtensionSharedParameters sharedParameters, AWTEvent event, boolean isNext) {
        searchInTabTitlesAndJump(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event), null, isNext);
    }

    public static void searchInTabTitlesAndJump(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, JMenuItem notificationMenuItem, boolean isNext) {
        if (!sharedParameters.searchedTabTitleForJumpToTab.isEmpty() && currentSubTabsContainerHandler != null) {
            boolean result = false;
            ArrayList<SubTabsContainerHandler> subTabsContainerHandlers = sharedParameters.allSubTabContainerHandlers.get(currentSubTabsContainerHandler.currentToolTab);
            ArrayList<SubTabsContainerHandler> tempSubTabsContainerHandlers;
            if (isNext) {
                tempSubTabsContainerHandlers = new ArrayList<>(subTabsContainerHandlers.subList(currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex(), subTabsContainerHandlers.size()));
            } else {
                tempSubTabsContainerHandlers = new ArrayList<>(subTabsContainerHandlers.subList(0, currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex()));
                Collections.reverse(tempSubTabsContainerHandlers);
            }

            for (SubTabsContainerHandler subTabsContainerHandlerItem : tempSubTabsContainerHandlers) {
                if ((subTabsContainerHandlerItem.getTabIndex() > subTabsContainerHandlerItem.parentTabbedPane.getSelectedIndex() && isNext)
                        || (subTabsContainerHandlerItem.getTabIndex() < subTabsContainerHandlerItem.parentTabbedPane.getSelectedIndex() && !isNext)) {
                    if (subTabsContainerHandlerItem.getVisible()) {
                        String subTabTitle = subTabsContainerHandlerItem.getTabTitle();
                        if (Pattern.compile(sharedParameters.searchedTabTitleForJumpToTab, Pattern.CASE_INSENSITIVE).matcher(subTabTitle).find()) {
                            jumpToTabIndex(sharedParameters, subTabsContainerHandlerItem, subTabsContainerHandlerItem.getTabIndex());
                            // This is because when we use mouse action, the menu won't be closed
                            if (notificationMenuItem != null)
                                setNotificationMenuMessage(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem, "Tab Title: " + currentSubTabsContainerHandler.parentTabbedPane.getTitleAt(currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex()).trim());
                            result = true;
                            break;
                        }
                    }
                }
            }
            if (result) {
                sharedParameters.printDebugMessage("Matched title was found");
                sharedParameters.printDebugMessage("Jumped to a title which matched: " + sharedParameters.searchedTabTitleForJumpToTab);
            } else {
                sharedParameters.printDebugMessage("No new match was found");
            }
        }
    }

    public static void jumpToFirstTab(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        jumpToFirstTab(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event));
    }

    public static void jumpToFirstTab(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        jumpToTabIndex(sharedParameters, currentSubTabsContainerHandler, 0);
        sharedParameters.printDebugMessage("Jump to first tab");
    }

    public static void jumpToLastTab(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        jumpToLastTab(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event));
    }

    public static void jumpToLastTab(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler == null)
            return;

        int maxIndex = currentSubTabsContainerHandler.parentTabbedPane.getTabCount() - 2;

        if (sharedParameters.isTabGroupSupportedByDefault)
            maxIndex += 1;

        jumpToTabIndex(sharedParameters, currentSubTabsContainerHandler, maxIndex);
        sharedParameters.printDebugMessage("Jump to last tab");
    }

    public static void jumpToPreviousTab(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        jumpToPreviousTab(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event), null);
    }

    public static void jumpToPreviousTab(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, JMenuItem notificationMenuItem) {
        if (currentSubTabsContainerHandler == null)
            return;
        if (currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex() > 0) {
            int chosenOne = currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex() - 1;

            SubTabsContainerHandler chosenOneSubTabsContainerHandler = getSubTabContainerHandlerFromSharedParameters(sharedParameters, currentSubTabsContainerHandler.parentTabbedPane, chosenOne);

            while (chosenOneSubTabsContainerHandler == null || !currentSubTabsContainerHandler.parentTabbedPane.isEnabledAt(chosenOne)
                    || !chosenOneSubTabsContainerHandler.isValid() || chosenOneSubTabsContainerHandler.isGroupContainerTab()
                    || !chosenOneSubTabsContainerHandler.isTitleVisible()) {
                chosenOne--;
                int maxIndex = currentSubTabsContainerHandler.parentTabbedPane.getTabCount();
                if (sharedParameters.isTabGroupSupportedByDefault)
                    maxIndex += 1;

                if (chosenOne < 0 || chosenOne >= maxIndex) {
                    chosenOne = currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex();
                    break;
                }
                chosenOneSubTabsContainerHandler = getSubTabContainerHandlerFromSharedParameters(sharedParameters, currentSubTabsContainerHandler.parentTabbedPane, chosenOne);
            }
            jumpToTabIndex(sharedParameters, currentSubTabsContainerHandler, chosenOne);
            // This is because when we use mouse action, the menu won't be closed
            if (notificationMenuItem != null)
                setNotificationMenuMessage(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem, "Tab Title: " + currentSubTabsContainerHandler.parentTabbedPane.getTitleAt(currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex()).trim());

            sharedParameters.printDebugMessage("Jump to previous tab");
        }
    }

    public static void jumpToNextTab(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        jumpToNextTab(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event), null);
    }

    public static void jumpToNextTab(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, JMenuItem notificationMenuItem) {
        if (currentSubTabsContainerHandler == null)
            return;
        int maxIndex = currentSubTabsContainerHandler.parentTabbedPane.getTabCount() - 2;

        if (sharedParameters.isTabGroupSupportedByDefault)
            maxIndex += 1;

        if (currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex() < maxIndex) {
            int chosenOne = currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex() + 1;
            SubTabsContainerHandler chosenOneSubTabsContainerHandler = getSubTabContainerHandlerFromSharedParameters(sharedParameters, currentSubTabsContainerHandler.parentTabbedPane, chosenOne);

            while (chosenOneSubTabsContainerHandler == null || !currentSubTabsContainerHandler.parentTabbedPane.isEnabledAt(chosenOne)
                    || !chosenOneSubTabsContainerHandler.isValid() || chosenOneSubTabsContainerHandler.isGroupContainerTab()
                    || !chosenOneSubTabsContainerHandler.isTitleVisible()) {
                chosenOne++;
                int maxIndex2 = currentSubTabsContainerHandler.parentTabbedPane.getTabCount();
                if (sharedParameters.isTabGroupSupportedByDefault)
                    maxIndex2 += 1;
                if (chosenOne < 0 || chosenOne >= maxIndex2) {
                    chosenOne = currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex();
                    break;
                }
                chosenOneSubTabsContainerHandler = getSubTabContainerHandlerFromSharedParameters(sharedParameters, currentSubTabsContainerHandler.parentTabbedPane, chosenOne);
            }

            jumpToTabIndex(sharedParameters, currentSubTabsContainerHandler, chosenOne);
            // This is because when we use mouse action, the menu won't be closed
            if (notificationMenuItem != null)
                setNotificationMenuMessage(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem, "Tab Title: " + currentSubTabsContainerHandler.parentTabbedPane.getTitleAt(currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex()).trim());

            sharedParameters.printDebugMessage("Jump to next tab");
        }
    }

    public static void jumpToPreviouslySelectedTab(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        jumpToPreviouslySelectedTab(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event), null);
    }

    public static void jumpToPreviouslySelectedTab(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, JMenuItem notificationMenuItem) {
        if (currentSubTabsContainerHandler == null)
            return;

        Integer previouslySelectedIndex = null;
        Integer currentSelectedIndex = sharedParameters.subTabPreviouslySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).pollLast();

        if (!currentSubTabsContainerHandler.isDotDotDotTab()) {
            previouslySelectedIndex = sharedParameters.subTabPreviouslySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).pollLast();
        }

        if (previouslySelectedIndex != null && currentSubTabsContainerHandler.parentTabbedPane.getTabComponentAt(previouslySelectedIndex) != null) {
            fixHistoryAndJumpToTabIndex(sharedParameters, currentSubTabsContainerHandler, previouslySelectedIndex, false, false, true);
            sharedParameters.printDebugMessage("Jump to previously selected tab");
        } else {
            sharedParameters.subTabPreviouslySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).add(currentSelectedIndex);
            sharedParameters.printDebugMessage("No previously selected tab was found");
        }

        if (notificationMenuItem != null)
            setNotificationMenuMessage(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem, "Tab Title: " + currentSubTabsContainerHandler.parentTabbedPane.getTitleAt(currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex()).trim());


    }

    public static void jumpToNextlySelectedTab(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        jumpToNextlySelectedTab(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event), null);
    }

    public static void jumpToNextlySelectedTab(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, JMenuItem notificationMenuItem) {
        if (currentSubTabsContainerHandler == null)
            return;

        Integer nextlySelectedIndex;
        nextlySelectedIndex = sharedParameters.subTabNextlySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).pollLast();

        if (nextlySelectedIndex != null && currentSubTabsContainerHandler.parentTabbedPane.getTabComponentAt(nextlySelectedIndex) != null) {
            fixHistoryAndJumpToTabIndex(sharedParameters, currentSubTabsContainerHandler, nextlySelectedIndex, false, true, true);
        }

        if (notificationMenuItem != null)
            setNotificationMenuMessage(sharedParameters, currentSubTabsContainerHandler, notificationMenuItem, "Tab Title: " + currentSubTabsContainerHandler.parentTabbedPane.getTitleAt(currentSubTabsContainerHandler.parentTabbedPane.getSelectedIndex()).trim());

        sharedParameters.printDebugMessage("Jump to previously selected tab");
    }

    public static void jumpToTabIndex(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, int indexNumber) {
        fixHistoryAndJumpToTabIndex(sharedParameters, currentSubTabsContainerHandler, indexNumber, true, true, true);
    }

    public static void fixHistoryAndJumpToTabIndex(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler, int indexNumber, boolean cleanNextlySelectedTabs, boolean ignoreNextlySelectedTabs, boolean shouldJump) {
        if (currentSubTabsContainerHandler == null)
            return;

        if (currentSubTabsContainerHandler.parentTabbedPane.getTabComponentAt(indexNumber) != null) {
            if (cleanNextlySelectedTabs) {
                sharedParameters.subTabNextlySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).clear();
            } else {
                if (!ignoreNextlySelectedTabs &&
                        (sharedParameters.subTabPreviouslySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).size() <= 0 ||
                                (sharedParameters.subTabPreviouslySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).size() > 0 &&
                                        sharedParameters.subTabPreviouslySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).getLast() != currentSubTabsContainerHandler.getTabIndex()))
                ) {
                    sharedParameters.subTabNextlySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab).add(currentSubTabsContainerHandler.getTabIndex());
                }

            }

            ShortcutMappings.recordSelectionHistory(
                    sharedParameters.subTabPreviouslySelectedIndexHistory.get(currentSubTabsContainerHandler.currentToolTab),
                    indexNumber,
                    currentSubTabsContainerHandler.parentTabbedPane.getTabCount() - 1 == indexNumber,
                    sharedParameters.isTabGroupSupportedByDefault);

            if (shouldJump) {
                // when the user is navigating from the tab header, the focus must stay there,
                // so the next arrow key press keeps navigating instead of typing in the editor
                boolean keepFocusOnHeader = ShortcutMappings.isTabAreaFocused(currentSubTabsContainerHandler.parentTabbedPane);

                try {
                    tabJumpInProgress = true;
                    currentSubTabsContainerHandler.parentTabbedPane.setSelectedIndex(indexNumber);
                } finally {
                    tabJumpInProgress = false;
                }

                if (keepFocusOnHeader) {
                    keepFocusOnTabHeader(sharedParameters, currentSubTabsContainerHandler.parentTabbedPane);
                }
            }
        }
    }

    // Incremented whenever the user deliberately moves the focus into the request editor
    // (the Down key). A pending "keep focus on header" retry captures this value and skips
    // itself when the value has changed, so it never steals focus back out of the editor.
    // Touched only on the EDT.
    private static long headerFocusEpoch = 0;

    // Moves the focus back to the selected tab header. Burp gives the focus to the
    // message editor after a tab change, so one delayed retry is used to win that race.
    // This is only called when the tab header already had the focus before the jump.
    // If the user presses Down before the retry runs, the retry is skipped, so it does
    // not pull the focus out of the editor.
    public static void keepFocusOnTabHeader(ExtensionSharedParameters sharedParameters, JTabbedPane tabbedPane) {
        final long epoch = headerFocusEpoch;

        SwingUtilities.invokeLater(() -> {
            if (headerFocusEpoch == epoch)
                requestFocusOnSelectedTabHeader(tabbedPane);
        });

        sharedParameters.delayedTasks.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if (sharedParameters.isUnloaded())
                            return;
                        SwingUtilities.invokeLater(() -> {
                            if (headerFocusEpoch == epoch && !ShortcutMappings.isTabAreaFocused(tabbedPane))
                                requestFocusOnSelectedTabHeader(tabbedPane);
                        });
                    }
                },
                250
        );
    }

    private static void requestFocusOnSelectedTabHeader(JTabbedPane tabbedPane) {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0)
            return;
        Component tabComponent = tabbedPane.getTabComponentAt(selectedIndex);
        if (tabComponent != null)
            tabComponent.requestFocusInWindow();
    }

    // Moves the focus from the tab header into the HTTP request editor of the selected tab,
    // so the user can start editing. Called by the fixed Down key on the tab header.
    public static void focusRequestEditor(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        SubTabsContainerHandler currentSubTabsContainerHandler = getSubTabContainerHandlerFromEvent(sharedParameters, event);
        if (currentSubTabsContainerHandler == null)
            return;

        Component content = currentSubTabsContainerHandler.parentTabbedPane.getSelectedComponent();
        if (!(content instanceof Container))
            return;

        Component editor = findRequestEditorComponent((Container) content);
        if (editor != null) {
            // invalidate any pending "keep focus on header" retry so it does not pull
            // the focus back out of the editor a moment after this jump
            headerFocusEpoch++;
            editor.requestFocusInWindow();
            sharedParameters.printDebugMessage("Focus moved into the request editor");
        }
    }

    // Burp's request editor is a custom component, not a Swing text component, so it cannot be
    // found by type or size reliably. Instead this looks at the point where a user would click to
    // edit the request (the left area of the tab content, below the toolbar) and takes the
    // component there, exactly like a real click. The request editor is on the left, the response
    // on the right, so a left-side point lands in the request.
    private static Component findRequestEditorComponent(Container content) {
        int width = content.getWidth();
        int height = content.getHeight();
        if (width <= 0 || height <= 0)
            return null;

        // left area (request, not response) and below the editor toolbar
        int pointX = Math.max(1, width / 6);
        int pointY = Math.max(1, (int) (height * 0.4));

        Component deepest = content.findComponentAt(pointX, pointY);

        // walk up to the first component that can take the keyboard focus, that is the editor
        Component candidate = deepest;
        while (candidate != null && candidate != content) {
            if (candidate.isFocusable() && candidate.isEnabled() && candidate.isShowing())
                return candidate;
            candidate = candidate.getParent();
        }
        return null;
    }

    // Moves the focus from the request or response editor back to the current tab header.
    // Called by the configurable Focus Tab key, which fires from anywhere in the window.
    public static void focusTab(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();

        JTabbedPane toolTabbedPane = null;
        if (focusOwner != null) {
            for (BurpUITools.MainTabs tool : sharedParameters.getAccessibleSubTabSupportedTabs()) {
                JTabbedPane pane = sharedParameters.get_toolTabbedPane(tool);
                if (pane != null && SwingUtilities.isDescendingFrom(focusOwner, pane)) {
                    toolTabbedPane = pane;
                    break;
                }
            }
        }

        // fallback to the pane that owns the key binding
        if (toolTabbedPane == null && event.getSource() instanceof Component sourceComponent) {
            toolTabbedPane = (JTabbedPane) UIWalker.findUIObjectInParentComponents(sourceComponent, 4, new UiSpecObject(JTabbedPane.class));
        }

        if (toolTabbedPane != null) {
            keepFocusOnTabHeader(sharedParameters, toolTabbedPane);
            sharedParameters.printDebugMessage("Focus moved back to the tab header");
        }
    }

    public static void copyTitle(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        copyTitle(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event));
    }

    public static void copyTitle(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler == null)
            return;

        String tabTitle = currentSubTabsContainerHandler.getTabTitle();
        // copying to clipboard as well
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(
                        new StringSelection(tabTitle),
                        null
                );
        sharedParameters.printDebugMessage("Title has been copied to clipboard");
    }

    public static void pasteTitle(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        pasteTitle(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event));
    }

    public static void pasteTitle(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler == null)
            return;

        // read off the EDT, then apply the title back on the EDT
        readClipboardTitleAsync(sharedParameters, clipboardTitle -> {
            if (!clipboardTitle.isBlank()) {
                currentSubTabsContainerHandler.setTabTitle(clipboardTitle, true);
                sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
                sharedParameters.printDebugMessage("Title has been pasted");
            }
        });
    }

    // Reads the clipboard text on a short background thread and hands the normalised
    // value to the consumer on the EDT. A synchronous read can block until the process
    // that owns the clipboard responds, so it must never run on the EDT.
    static void readClipboardTitleAsync(ExtensionSharedParameters sharedParameters, Consumer<String> onResultOnEdt) {
        new Thread(() -> {
            String clipboardText;
            try {
                clipboardText = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(DataFlavor.stringFlavor);
            } catch (Exception e) {
                clipboardText = null;
            }
            String normalizedTitle = normalizeClipboardTitle(clipboardText);
            sharedParameters.lastClipboardText = normalizedTitle;
            SwingUtilities.invokeLater(() -> onResultOnEdt.accept(normalizedTitle));
        }, "SharpenerClipboardRead").start();
    }

    // Trims the clipboard value and removes the "#<number> " prefix that a copied
    // numbered tab title starts with. Returns an empty string for a null value.
    static String normalizeClipboardTitle(String clipboardText) {
        if (clipboardText == null) {
            return "";
        }
        return clipboardText.trim().replaceAll("^#\\d+\\s+", "");
    }

    public static void renameTitle(ExtensionSharedParameters sharedParameters, ActionEvent event) {
        renameTitle(sharedParameters, getSubTabContainerHandlerFromEvent(sharedParameters, event));
    }

    public static void renameTitle(ExtensionSharedParameters sharedParameters, SubTabsContainerHandler currentSubTabsContainerHandler) {
        if (currentSubTabsContainerHandler == null)
            return;

        String newTitle = UIHelper.showPlainInputMessage("Edit the Title", "Rename Title", currentSubTabsContainerHandler.getTabTitle(), sharedParameters.get_mainFrameUsingMontoya());
        if (!newTitle.isEmpty() && !newTitle.equals(currentSubTabsContainerHandler.getTabTitle())) {
            currentSubTabsContainerHandler.setTabTitle(newTitle, true);
            sharedParameters.allSettings.subTabsSettings.saveSettings(currentSubTabsContainerHandler);
            sharedParameters.printDebugMessage("Title renamed...");
        }
    }
}
