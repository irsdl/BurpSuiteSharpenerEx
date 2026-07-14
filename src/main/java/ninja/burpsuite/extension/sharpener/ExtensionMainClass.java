// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.websocket.ProxyWebSocketCreationHandler;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilityGroup;
import ninja.burpsuite.extension.sharpener.uiSelf.contextMenu.ContextMenu;
import ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor.ExtensionHttpRequestEditorProvider;
import ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor.ExtensionHttpResponseEditorProvider;
import ninja.burpsuite.extension.sharpener.uiSelf.suiteTab.SuiteTab;
import ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenu;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExtensionMainClass implements BurpExtension, ExtensionUnloadingHandler {
    private ExtensionSharedParameters sharedParameters = null;
    private Boolean isActive = null;
    private PropertyChangeListener lookAndFeelPropChangeListener;

    @Override
    public void initialize(MontoyaApi api) {
        this.sharedParameters = new ExtensionSharedParameters(this, api, "/extension.properties");

        // set our extension name
        api.extension().setName(sharedParameters.extensionName);
        // registering itself to handle unloading
        api.extension().registerUnloadingHandler(this);

        if (!sharedParameters.isCompatibleWithCurrentBurpVersion) {
            // This is not a compatible extension, what should we do?
            UIHelper.showWarningMessage("The " + sharedParameters.extensionName +
                    " extension is not compatible with the current version or edition of Burp Suite" +
                    "\nPlease look at the extension errors for more details.", sharedParameters.get_rootTabbedPaneUsingMontoya());
            api.extension().unload();
        }

        furtherLoadingChecks();

        if (sharedParameters.features.hasHttpHandler) {
            // register our HTTP handlers - the condition above will always be true in the Sharpener extension
            for (var capabilitySettings : sharedParameters.allSettings.capabilitySettingsList) {
                if (capabilitySettings.capability.capabilityGroupList.contains(CapabilityGroup.PROXY_REQUEST_HANDLER)) {
                    // we need to register a proxy request handler using the item.capability.createCapabilityHandler() object
                    api.proxy().registerRequestHandler((ProxyRequestHandler) capabilitySettings.capability.createCapabilityObject(sharedParameters, capabilitySettings));
                }

                if (capabilitySettings.capability.capabilityGroupList.contains(CapabilityGroup.PROXY_RESPONSE_HANDLER)) {
                    // we need to register a proxy request handler using the item.capability.createCapabilityHandler() object
                    api.proxy().registerResponseHandler((ProxyResponseHandler) capabilitySettings.capability.createCapabilityObject(sharedParameters, capabilitySettings));
                }

                if (capabilitySettings.capability.capabilityGroupList.contains(CapabilityGroup.WEBSOCKET_CREATION_HANDLER)) {
                    // we need to register a proxy request handler using the item.capability.createCapabilityHandler() object
                    api.proxy().registerWebSocketCreationHandler((ProxyWebSocketCreationHandler) capabilitySettings.capability.createCapabilityObject(sharedParameters, capabilitySettings));
                }
            }
        }

        // create our UI
        // we no longer need to create an extension GUI tab to get access to the jFrame - Montoya can give us access
        if (sharedParameters.features.hasSuiteTab) {
            sharedParameters.extensionSuiteTab = new SuiteTab(sharedParameters);
            sharedParameters.extensionSuiteTabRegistration = api.userInterface().registerSuiteTab(sharedParameters.extensionName, sharedParameters.extensionSuiteTab);
        }

        if (sharedParameters.features.hasContextMenu) {
            sharedParameters.extensionMainContextMenu = new ContextMenu(sharedParameters);
            sharedParameters.extensionContextMenuRegistration = api.userInterface().registerContextMenuItemsProvider(sharedParameters.extensionMainContextMenu);
        }

        if (sharedParameters.features.hasTopMenu) {
            sharedParameters.topMenuBar = new TopMenu(sharedParameters);
            sharedParameters.extensionTopMenuRegistration = api.userInterface().menuBar().registerMenu(sharedParameters.topMenuBar);

            // After a look and feel (theme) change and reload, the freshly added menu can be given a
            // zero size because its UI delegate is not installed for the current theme, so it does not
            // paint even though it is in the menu bar. Reinstall its UI and re-lay out the menu bar.
            SwingUtilities.invokeLater(() -> {
                try {
                    sharedParameters.topMenuBar.updateUI();
                    JMenuBar mainMenuBar = sharedParameters.get_mainMenuBarUsingMontoya();
                    if (mainMenuBar != null) {
                        mainMenuBar.revalidate();
                        mainMenuBar.repaint();
                    }
                } catch (Exception e) {
                    sharedParameters.printDebugMessage("Could not refresh the top menu layout: " + e.getMessage());
                }
            });
        }

        if (sharedParameters.features.hasHttpRequestEditor) {
            api.userInterface().registerHttpRequestEditorProvider(new ExtensionHttpRequestEditorProvider(sharedParameters));
        }

        if (sharedParameters.features.hasHttpResponseEditor) {
            api.userInterface().registerHttpResponseEditorProvider(new ExtensionHttpResponseEditorProvider(sharedParameters));
        }

        sharedParameters.printlnOutput(sharedParameters.extensionName + " has been loaded successfully.");
    }

    public boolean getIsActive() {
        if (this.isActive == null)
            setIsActive(false);
        return this.isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public void extensionUnloaded() {
        unload();
    }

    private void initializeSettings() {
        sharedParameters.printDebugMessage("Loading all settings!");
        // Loading all settings!
        sharedParameters.allSettings = new ExtensionGeneralSettings(sharedParameters);
    }

    public void furtherLoadingChecks() {
        sharedParameters.printDebugMessage("Performing further loading checks!");
        try {
            sharedParameters.setUIParametersUsingMontoya(10);

            if (!sharedParameters.get_isUILoaded()) {
                sharedParameters.printlnError("UI cannot be loaded... try again");
                sharedParameters.montoyaApi.extension().unload();
                return;
            }

            // A leftover Sharpener menu can still be present right after a quick unload plus reload,
            // because Burp removes the old menu only after the previous unload has finished.
            // Our own menu is registered later, so any menu found now is stale.
            // We remove it and keep loading instead of unloading ourselves. The old code unloaded here
            // after the settings were already loaded, which left a half loaded extension behind.
            if (BurpUITools.isMenuBarLoaded(sharedParameters.extensionName, sharedParameters.get_mainMenuBarUsingMontoya())) {
                sharedParameters.printlnError("A leftover " + sharedParameters.extensionName + " menu was found. Removing it and continuing to load.");
                BurpUITools.removeMenuBarByName(sharedParameters.extensionName, sharedParameters.get_mainMenuBarUsingMontoya(), true);
            }

            // This needs to be initialized after the UI is accessible
            initializeSettings();

            // This is a dirty hack when LookAndFeel changes in the middle, and we lose the style!
            lookAndFeelPropChangeListener = evt -> {
                // only react to the actual look and feel switch, not every UIManager change
                if (evt.getPropertyName() == null || !evt.getPropertyName().equals("lookAndFeel"))
                    return;
                sharedParameters.unloadWithoutSave = true; // we need to unload the extension without saving it as major change in UI has occurred (switch to dark/light mode)
                sharedParameters.delayedTasks.schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                if (sharedParameters.isUnloaded())
                                    return;
                                SwingUtilities.invokeLater(() -> {
                                    if (sharedParameters.isUnloaded())
                                        return;
                                    sharedParameters.printDebugMessage("lookAndFeelPropChangeListener");
                                    sharedParameters.defaultTabFeaturesObjectStyle = null;
                                    try {
                                        BurpUITools.switchToMainTab(BurpUITools.MainTabs.Extensions.toString(), sharedParameters.get_rootTabbedPaneUsingMontoya());
                                    } catch (Exception ex) {
                                        sharedParameters.printDebugMessage("Could not switch to the extensions tab: " + ex.getMessage());
                                    }
                                    try {
                                        // shown on the EDT so it renders correctly during the theme change; the old code used a
                                        // background thread which drew an empty dialog while the look and feel was still switching
                                        JOptionPane.showMessageDialog(sharedParameters.get_mainFrameUsingMontoya(),
                                                "Due to a major UI change, the " + sharedParameters.extensionName + " extension needs to be unloaded. Please load it manually.",
                                                "Warning", JOptionPane.WARNING_MESSAGE);
                                    } catch (Throwable ignored) {
                                        // never let a broken dialog stop the unload below
                                    }
                                    // the unload must always run, even if the message or tab switch above failed
                                    sharedParameters.montoyaApi.extension().unload();
                                });
                            }
                        },
                        2000
                );
            };

            sharedParameters.printDebugMessage("addPropertyChangeListener: lookAndFeelPropChangeListener");
            UIManager.addPropertyChangeListener(lookAndFeelPropChangeListener);

        } catch (Exception e) {
            sharedParameters.printlnError("Fatal error in loading the extension");
            sharedParameters.printException(e);
        }
    }

    public void unload() {
        sharedParameters.printDebugMessage("unload");
        // stop all delayed tasks first so nothing fires against the dead Burp API while we clean up
        sharedParameters.delayedTasks.stop();
        try {
            /*
            // reattaching related tools before working on them!
            if (BurpUITools.reattachTools(sharedParameters.subTabSupportedTabs, sharedParameters.get_mainMenuBarUsingMontoya())) {

                try {
                    sharedParameters.printDebugMessage("reattaching");
                    // to make sure UI has been updated
                    sharedParameters.printlnOutput("Detached windows were found. We need to wait for a few seconds after reattaching the tabs.");
                    Thread.sleep(3000);
                } catch (Exception e) {
                    sharedParameters.printDebugMessage("Error in reattaching the tools");
                }
            }
            */
            if (sharedParameters.get_isUILoaded()) {
                try {
                    sharedParameters.printDebugMessage("removePropertyChangeListener: lookAndFeelPropChangeListener");
                    UIManager.removePropertyChangeListener(lookAndFeelPropChangeListener);

                    sharedParameters.allSettings.unloadSettings();

                    // sometimes the UI is still being updated because of SwingUtilities.invokeLater,
                    // so we flush the pending EDT work instead of blocking on a fixed sleep
                    if (!SwingUtilities.isEventDispatchThread()) {
                        try {
                            SwingUtilities.invokeAndWait(() -> {
                            });
                        } catch (Exception flushError) {
                            // ignore, we are unloading anyway
                        }
                    }

                } catch (Exception e) {
                    sharedParameters.printlnError("An error has occurred when unloading the " + sharedParameters.extensionName + " extension.");
                    sharedParameters.printDebugMessage(e.getMessage());
                    sharedParameters.montoyaApi.logging().logToError(e);
                    UIHelper.showWarningMessage(sharedParameters.extensionName + " extension has been closed with an error.\r\n" +
                                    "You may need to restart Burp Suite.\r\n" +
                                    "Please consider looking at the error and reporting it to the GitHub repository:\r\n" +
                                    sharedParameters.extensionURL
                            , sharedParameters.topMenuBar);
                }
            }

            sharedParameters.printDebugMessage("UI changes have been removed.");
        } catch (Exception e) {
            sharedParameters.printlnError("Fatal error in unloading the extension");
            sharedParameters.printException(e);
        }
        System.gc(); // probably will be ignored by Java anyway!
    }

    public void checkForUpdate() {
        // we need to see whether the extension is up-to-date by reading the propertiesFileUrl link
        new Thread(() -> {
            try {
                boolean isError = true;

                var buildGradleFileResponse = sharedParameters.montoyaApi.http().sendRequest(HttpRequest.httpRequestFromUrl(sharedParameters.extensionPropertiesUrl));

                var propertiesFile = buildGradleFileResponse.response().body().getBytes();

                if (propertiesFile != null) {
                    String buildGradleFileStr = new String(propertiesFile);
                    Pattern version_Pattern = Pattern.compile("version=([\\d.]+)");
                    Matcher m = version_Pattern.matcher(buildGradleFileStr);
                    if (m.find()) {
                        String githubVersionStr = m.group(1);
                        try {
                            double currentVersion = Double.parseDouble(sharedParameters.version);
                            double githubVersion = Double.parseDouble(githubVersionStr);

                            if (githubVersion > currentVersion) {
                                sharedParameters.printlnOutput(sharedParameters.extensionName + " is outdated. The latest version is: " + githubVersionStr);
                                new Thread(() -> {
                                    int answer = UIHelper.askConfirmMessage("A new version of " + sharedParameters.extensionName + " is available", "Do you want to open the " + sharedParameters.extensionName + " project page to download the latest version?", new String[]{"Yes", "No"}, sharedParameters.get_mainFrameUsingMontoya());
                                    if (answer == 0) {
                                        try {
                                            Desktop.getDesktop().browse(new URI(sharedParameters.extensionURL + "/releases/"));
                                        } catch (Exception e) {
                                            sharedParameters.printlnError(e.getMessage());
                                        }
                                    }
                                }).start();

                            } else if (currentVersion > githubVersion) {
                                sharedParameters.printlnOutput(sharedParameters.extensionName + " is more than up to date; do you have a time machine?");
                            } else {
                                sharedParameters.printlnOutput(sharedParameters.extensionName + " is up to date");
                            }
                            isError = false;
                        } catch (Exception e) {
                            sharedParameters.printDebugMessage("Error in SharpenerBurpExtender.checkForUpdate()" + e.getMessage());
                        }
                    }
                }
                if (isError) {
                    sharedParameters.printlnError("Could not check for update from " + sharedParameters.extensionPropertiesUrl);
                }
            } catch (Exception e) {
                sharedParameters.printlnError("Fatal error in checkForUpdate()");
                sharedParameters.printException(e);
            }
        }).start();
    }
}
