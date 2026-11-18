// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.http.Http;
import burp.api.montoya.http.RequestOptions;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.websocket.ProxyWebSocketCreationHandler;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilityGroup;
import ninja.burpsuite.extension.sharpener.uiSelf.contextMenu.ContextMenu;
import ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor.ExtensionHttpRequestEditorProvider;
import ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor.ExtensionHttpResponseEditorProvider;
import ninja.burpsuite.extension.sharpener.uiControllers.subTabs.SubTabsActions;
import ninja.burpsuite.extension.sharpener.uiSelf.suiteTab.SuiteTab;
import ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenu;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.burp.generic.BurpUIDiscovery;
import ninja.burpsuite.libs.generic.ResourceIconCache;
import ninja.burpsuite.libs.generic.SingleInstanceGuard;
import ninja.burpsuite.libs.generic.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExtensionMainClass implements BurpExtension, ExtensionUnloadingHandler {
    // root pane client property key for the SingleInstanceGuard liveness marker
    public static final String INSTANCE_MARKER_KEY = "ninja.burpsuite.extension.sharpener.loadedInstance";

    private ExtensionSharedParameters sharedParameters = null;
    private Boolean isActive = null;
    private PropertyChangeListener lookAndFeelPropChangeListener;
    // true when another live copy of this extension was detected, so this copy
    // must unload itself without touching Burp's UI or the saved settings
    private boolean isDuplicateInstance = false;
    // this copy's own liveness marker, kept so unload removes exactly this marker
    private BooleanSupplier ownInstanceMarker = null;
    private JRootPane instanceRootPane;
    private final AtomicBoolean updateCheckRunning = new AtomicBoolean();

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
            return;
        }

        // Do not register handlers or UI after a failed or aborted startup.
        if (!furtherLoadingChecks())
            return;

        // Load the menu icons through the extension-owned background queue,
        // so neither extension loading nor the EDT pays for the first icon scan.
        // ResourceIconCache is thread safe, an early menu open simply waits for it.
        if (sharedParameters.extensionClass != null) sharedParameters.backgroundTasks.submit("icon cache warm-up", () -> {
            if (sharedParameters.isUnloaded()) return;
            ResourceIconCache.getIcons(sharedParameters.extensionClass,
                    TopMenu.BURP_ICON_FOLDER, TopMenu.BURP_ICON_MENU_WIDTH);
            ResourceIconCache.getIcons(sharedParameters.extensionClass,
                    SubTabsActions.SUB_TAB_ICON_FOLDER, SubTabsActions.SUB_TAB_ICON_MENU_WIDTH);
        });

        if (sharedParameters.features.hasHttpHandler) {
            // register our HTTP handlers - the condition above will always be true in the Sharpener extension
            for (var capabilitySettings : sharedParameters.allSettings.capabilitySettingsList) {
                sharedParameters.runOptionalFeature(capabilitySettings.getClass().getSimpleName() + " handlers", () -> {
                    Object handler = capabilitySettings.capability.createCapabilityObject(sharedParameters, capabilitySettings);
                    if (handler == null) return;
                    if (capabilitySettings.capability.capabilityGroupList.contains(CapabilityGroup.PROXY_REQUEST_HANDLER)) {
                        // we need to register a proxy request handler using the item.capability.createCapabilityHandler() object
                        api.proxy().registerRequestHandler((ProxyRequestHandler) handler);
                    }

                    if (capabilitySettings.capability.capabilityGroupList.contains(CapabilityGroup.PROXY_RESPONSE_HANDLER)) {
                        // we need to register a proxy request handler using the item.capability.createCapabilityHandler() object
                        api.proxy().registerResponseHandler((ProxyResponseHandler) handler);
                    }

                    if (capabilitySettings.capability.capabilityGroupList.contains(CapabilityGroup.WEBSOCKET_CREATION_HANDLER)) {
                        // we need to register a proxy request handler using the item.capability.createCapabilityHandler() object
                        api.proxy().registerWebSocketCreationHandler((ProxyWebSocketCreationHandler) handler);
                    }
                    if (capabilitySettings.capability.capabilityGroupList.contains(CapabilityGroup.HTTP_HANDLER)) {
                        api.http().registerHttpHandler((burp.api.montoya.http.handler.HttpHandler)
                                handler);
                    }
                });
            }
        }

        BurpUIDiscovery.onEdt(() -> {
            registerOptionalUi(api);
            sharedParameters.runOptionalFeature("UI layout recovery", sharedParameters.allSettings::startUiMonitoring);
            return null;
        });

        sharedParameters.printlnOutput(sharedParameters.extensionName + " has been loaded. See extension errors for any unavailable features.");
    }

    private void registerOptionalUi(MontoyaApi api) {
        // create our UI
        // we no longer need to create an extension GUI tab to get access to the jFrame - Montoya can give us access
        if (sharedParameters.features.hasSuiteTab) {
            sharedParameters.runOptionalFeature("suite tab", () -> {
                sharedParameters.extensionSuiteTab = new SuiteTab(sharedParameters);
                sharedParameters.extensionSuiteTabRegistration = api.userInterface().registerSuiteTab(sharedParameters.extensionName, sharedParameters.extensionSuiteTab);
            });
        }

        if (sharedParameters.features.hasContextMenu) {
            sharedParameters.runOptionalFeature("context menu", () -> {
                sharedParameters.extensionMainContextMenu = new ContextMenu(sharedParameters);
                sharedParameters.extensionContextMenuRegistration = api.userInterface().registerContextMenuItemsProvider(sharedParameters.extensionMainContextMenu);
            });
        }

        if (sharedParameters.features.hasTopMenu) {
            sharedParameters.runOptionalFeature("top menu", () -> {
                sharedParameters.topMenuBar = new TopMenu(sharedParameters);
                sharedParameters.extensionTopMenuRegistration = api.userInterface().menuBar().registerMenu(sharedParameters.topMenuBar);

                // After a look and feel (theme) change and reload, the freshly added menu can be given a
                // zero size because its UI delegate is not installed for the current theme, so it does not
                // paint even though it is in the menu bar. Reinstall its UI and re-lay out the menu bar.
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (sharedParameters.isUnloaded()) return;
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
            });
        }

        if (sharedParameters.features.hasHttpRequestEditor) {
            sharedParameters.runOptionalFeature("request editor", () -> {
                api.userInterface().registerHttpRequestEditorProvider(new ExtensionHttpRequestEditorProvider(sharedParameters));
            });
        }

        if (sharedParameters.features.hasHttpResponseEditor) {
            sharedParameters.runOptionalFeature("response editor", () ->
                    api.userInterface().registerHttpResponseEditorProvider(new ExtensionHttpResponseEditorProvider(sharedParameters)));
        }
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
        if (!sharedParameters.allSettings.isLoaded())
            throw new IllegalStateException("Core extension settings could not be loaded");
    }

    public boolean furtherLoadingChecks() {
        sharedParameters.printDebugMessage("Performing further loading checks!");
        try {
            sharedParameters.runOptionalFeature("Burp frame discovery", () -> sharedParameters.setUIParametersUsingMontoya(10));
            return BurpUIDiscovery.onEdt(this::loadSettingsOnEdt);
        } catch (RuntimeException | LinkageError e) {
            sharedParameters.printlnError("Fatal error in loading the extension");
            sharedParameters.montoyaApi.logging().logToError(e);
            sharedParameters.montoyaApi.extension().unload();
            return false;
        }
    }

    private boolean loadSettingsOnEdt() {
        try {

            // Duplicate load detection. A menu bar check cannot tell a stale leftover menu
            // (quick unload plus reload race) from a second live copy, so a liveness marker
            // on Burp's root pane is checked instead: the previous copy's marker turns dead
            // as soon as its unload starts, while a genuinely loaded copy stays live.
            JFrame frame = sharedParameters.get_mainFrameUsingMontoya();
            JRootPane burpRootPane = frame == null ? null : frame.getRootPane();
            if (burpRootPane != null && SingleInstanceGuard.isAnotherInstanceLive(burpRootPane, INSTANCE_MARKER_KEY)) {
                isDuplicateInstance = true;
                String errMessage = "Another live copy of the " + sharedParameters.extensionName +
                        " extension is already loaded. This copy will now be unloaded.";
                sharedParameters.printlnError(errMessage);
                UIHelper.showWarningMessage(errMessage, sharedParameters.get_mainFrameUsingMontoya());
                sharedParameters.montoyaApi.extension().unload();
                return false;
            }

            // A leftover Sharpener menu can still be present right after a quick unload plus reload,
            // because Burp removes the old menu only after the previous unload has finished.
            // Our own menu is registered later, so any menu found now is stale.
            // We remove it and keep loading instead of unloading ourselves. The old code unloaded here
            // after the settings were already loaded, which left a half loaded extension behind.
            sharedParameters.runOptionalFeature("stale menu cleanup", () -> {
                JMenuBar bar = sharedParameters.get_mainMenuBarUsingMontoya();
                if (BurpUITools.isMenuBarLoaded(sharedParameters.extensionName, bar))
                    BurpUITools.removeMenuBarByName(sharedParameters.extensionName, bar, true);
            });

            if (burpRootPane != null) {
                ownInstanceMarker = () -> !sharedParameters.isUnloaded();
                instanceRootPane = burpRootPane;
                SingleInstanceGuard.publish(burpRootPane, INSTANCE_MARKER_KEY, ownInstanceMarker);
            } else {
                sharedParameters.reportCompatibilityIssue("frame customisation and duplicate-instance UI guard", null);
            }

            // Preferences and API features still load when optional Swing structures are missing.
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
                                        JFrame parentFrame = sharedParameters.get_mainFrameUsingMontoya();
                                        if (parentFrame != null) {
                                            JOptionPane.showMessageDialog(parentFrame,
                                                    "Due to a major UI change, the " + sharedParameters.extensionName + " extension needs to be unloaded. Please load it manually.",
                                                    "Warning", JOptionPane.WARNING_MESSAGE);
                                        }
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
            return true;

        } catch (Exception e) {
            sharedParameters.printlnError("Fatal error in loading the extension");
            sharedParameters.printException(e);
            sharedParameters.montoyaApi.extension().unload();
            return false;
        }
    }

    public void unload() {
        sharedParameters.printDebugMessage("unload");
        // stop all delayed tasks first so nothing fires against the dead Burp API while we clean up
        sharedParameters.delayedTasks.stop();
        if (sharedParameters.backgroundTasks != null) sharedParameters.backgroundTasks.stop();

        // a duplicate copy never published a marker, never loaded settings, and never
        // touched Burp's UI, so it must not run the restore logic of the live copy
        if (isDuplicateInstance) {
            sharedParameters.printDebugMessage("Duplicate copy unloaded without touching the UI.");
            return;
        }

        // remove this copy's liveness marker so a later load never sees a dead leftover
        // marker, and so the marker does not keep the unloaded classloader alive
        try {
            BurpUIDiscovery.onEdt(() -> {
                if (instanceRootPane != null)
                    SingleInstanceGuard.clear(instanceRootPane, INSTANCE_MARKER_KEY, ownInstanceMarker);
                instanceRootPane = null;
                return null;
            });
        } catch (Exception e) {
            sharedParameters.printDebugMessage("Could not clear the instance marker: " + e.getMessage());
        }

        try {
            UIManager.removePropertyChangeListener(lookAndFeelPropChangeListener);
            if (sharedParameters.allSettings != null)
                BurpUIDiscovery.onEdt(() -> {
                    sharedParameters.allSettings.unloadSettings();
                    return null;
                });
            sharedParameters.printDebugMessage("UI changes have been removed.");
        } catch (RuntimeException | LinkageError e) {
            sharedParameters.reportCompatibilityIssue("extension cleanup", e);
        }
    }

    // The update check must go through Burp networking with upstream TLS verification
    // turned on, so a network level attacker cannot substitute the remote version file.
    // Burp does not verify the upstream certificate unless this option is set.
    static HttpRequestResponse sendVersionCheckRequest(Http http, HttpRequest request, RequestOptions requestOptions) {
        return http.sendRequest(request, requestOptions.withResponseTimeout(15000).withUpstreamTLSVerification());
    }

    public void checkForUpdate() {
        // we need to see whether the extension is up-to-date by reading the propertiesFileUrl link
        if (sharedParameters.isUnloaded() || !updateCheckRunning.compareAndSet(false, true)) return;
        boolean submitted = sharedParameters.backgroundTasks.submit("update check", () -> {
            try {
                boolean isError = true;

                var buildGradleFileResponse = sendVersionCheckRequest(sharedParameters.montoyaApi.http(),
                        HttpRequest.httpRequestFromUrl(sharedParameters.extensionPropertiesUrl),
                        RequestOptions.requestOptions());

                if (sharedParameters.isUnloaded() || sharedParameters.backgroundTasks.isStopped()) return;
                if (buildGradleFileResponse == null || buildGradleFileResponse.response() == null
                        || buildGradleFileResponse.response().statusCode() != 200
                        || buildGradleFileResponse.response().body().length() > 65536)
                    throw new IllegalStateException("Invalid version response");

                var propertiesFile = buildGradleFileResponse.response().body().getBytes();

                if (propertiesFile != null) {
                    String buildGradleFileStr = new String(propertiesFile, StandardCharsets.UTF_8);
                    Pattern version_Pattern = Pattern.compile("(?m)^version=([0-9]+\\.[0-9]+)\\s*$");
                    Matcher m = version_Pattern.matcher(buildGradleFileStr);
                    if (m.find()) {
                        String githubVersionStr = m.group(1);
                        try {
                            double currentVersion = Double.parseDouble(sharedParameters.version);
                            double githubVersion = Double.parseDouble(githubVersionStr);

                            if (githubVersion > currentVersion) {
                                sharedParameters.printlnOutput(sharedParameters.extensionName + " is outdated. The latest version is: " + githubVersionStr);
                                SwingUtilities.invokeLater(() -> {
                                    if (sharedParameters.isUnloaded() || sharedParameters.backgroundTasks.isStopped()) return;
                                    if (sharedParameters.get_mainFrameUsingMontoya() == null) return;
                                    int answer = UIHelper.askConfirmMessage("A new version of " + sharedParameters.extensionName + " is available", "Do you want to open the " + sharedParameters.extensionName + " project page to download the latest version?", new String[]{"Yes", "No"}, sharedParameters.get_mainFrameUsingMontoya());
                                    if (answer == 0 && !sharedParameters.isUnloaded()) {
                                        sharedParameters.backgroundTasks.submit("open releases page", () -> {
                                            Desktop.getDesktop().browse(new URI(sharedParameters.extensionURL + "/releases/"));
                                        });
                                    }
                                });

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
                if (!sharedParameters.isUnloaded()) sharedParameters.reportBackgroundFailure("update check", e);
            } finally {
                updateCheckRunning.set(false);
            }
        });
        if (!submitted) updateCheckRunning.set(false);
    }
}
