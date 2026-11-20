// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.uiSelf.settings;

import burp.api.montoya.core.Registration;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.settings.SettingsPanel;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

/** All direct references to optional host methods/types stay behind this class boundary. */
public final class NativeSettingsIntegration {
    private NativeSettingsIntegration() { }
    public static SettingsSurface.NativeSurface register(ExtensionSharedParameters p) throws ReflectiveOperationException {
        UserInterface.class.getMethod("registerSettingsPanel", SettingsPanel.class);
        SharpenerSettingsPanel panel = new SharpenerSettingsPanel(p);
        Registration registration = null;
        try {
            registration = p.montoyaApi.userInterface().registerSettingsPanel(panel);
            if (registration == null) throw new IllegalStateException("Settings registration returned no handle");
            if (!registration.isRegistered()) throw new IllegalStateException("Settings panel was not registered");
            boolean navigation;
            try { UserInterface.class.getMethod("openSettingsWindow"); navigation = true; }
            catch (NoSuchMethodException | LinkageError absent) { navigation = false; }
            return new SettingsSurface.NativeSurface(registration, panel.uiComponent(), panel, navigation,
                    () -> p.montoyaApi.userInterface().openSettingsWindow());
        } catch (RuntimeException | LinkageError failure) {
            if (registration != null) try { registration.deregister(); } catch (RuntimeException | LinkageError cleanup) {
                failure.addSuppressed(cleanup); panel.close(); throw new SettingsSurface.FailedCleanup(registration, failure);
            }
            panel.close(); throw failure;
        }
    }
}
