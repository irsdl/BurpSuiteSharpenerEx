// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.uiSelf.settings;

import burp.api.montoya.core.Registration;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenu;
import javax.swing.*;

/** Full menu on every host, plus an independent optional Settings panel. EDT only. */
public final class SettingsSurface implements AutoCloseable {
    public static final class FailedCleanup extends RuntimeException {
        private final Registration registration;
        FailedCleanup(Registration registration, Throwable cause) {
            super("Host did not remove a partial settings registration; cannot safely add a second surface", cause);
            this.registration = registration;
        }
    }
    public record NativeSurface(Registration registration, JComponent component, AutoCloseable cleanup,
                                boolean navigation, Runnable open) { }
    @FunctionalInterface public interface NativeRegistrar { NativeSurface register(ExtensionSharedParameters p) throws ReflectiveOperationException; }
    private final ExtensionSharedParameters p;
    private NativeSurface nativeSurface;
    private Registration menuRegistration;
    private Registration partialRegistration;
    private boolean navigation;
    private boolean closed;
    private boolean started;
    public SettingsSurface(ExtensionSharedParameters p) { this.p = p; }
    public void register() { register(parameters -> NativeSettingsIntegration.register(parameters)); }
    public void register(NativeRegistrar registrar) {
        if (closed || p.isUnloaded() || started) return;
        started = true;
        registerMenu();
        if (closed || p.isUnloaded()) return;
        try {
            NativeSurface candidate = registrar.register(p);
            if (candidate == null) throw new IllegalStateException("No native settings surface");
            if (closed || p.isUnloaded()) { cleanupNative(candidate); return; }
            nativeSurface = candidate; navigation = candidate.navigation();
        } catch (FailedCleanup failure) {
            partialRegistration = failure.registration;
            p.reportCompatibilityIssue("partial native settings registration cleanup", failure);
            if (closed || p.isUnloaded()) cleanupPartialRegistration();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            p.reportCompatibilityIssue("optional native Sharpener settings (use the Sharpener menu)", failure);
        }
    }
    private void registerMenu() {
        try {
            p.topMenuBar = new TopMenu(p);
            menuRegistration = p.montoyaApi.userInterface().menuBar().registerMenu(p.topMenuBar);
            if (menuRegistration == null || !menuRegistration.isRegistered()) throw new IllegalStateException("Menu registration failed");
            if (closed || p.isUnloaded()) { cleanupMenu(); return; }
            p.extensionTopMenuRegistration = menuRegistration;
            p.topMenuBar.updateUI();
            JMenuBar bar = p.get_mainMenuBarUsingMontoya();
            if (bar != null) { bar.revalidate(); bar.repaint(); }
        } catch (RuntimeException | LinkageError failure) {
            cleanupMenu(); p.reportCompatibilityIssue("Sharpener top menu", failure);
        }
    }
    public boolean isNative() { return nativeSurface != null && !closed; }
    public JComponent component() { return isNative() ? nativeSurface.component() : p.topMenuBar; }
    public boolean canOpen() { return isNative() && navigation; }
    public void open() {
        if (!canOpen() || p.isUnloaded()) return;
        try { nativeSurface.open().run(); }
        catch (RuntimeException | LinkageError failure) { navigation = false; p.reportCompatibilityIssue("Settings navigation (use Burp Settings > Extensions > Sharpener)", failure); }
    }
    private void cleanupMenu() {
        if (menuRegistration != null) {
            try { menuRegistration.deregister(); } catch (RuntimeException | LinkageError failure) { p.reportCompatibilityIssue("settings menu cleanup", failure); }
            menuRegistration = null;
        }
        if (p.topMenuBar != null) { p.topMenuBar.close();
            if (p.topMenuBar.getParent() instanceof JMenuBar bar) { bar.remove(p.topMenuBar); bar.revalidate(); bar.repaint(); }
            p.topMenuBar = null;
        }
    }
    private void cleanupNative(NativeSurface surface) {
        try { surface.registration().deregister(); } catch (RuntimeException | LinkageError failure) { p.reportCompatibilityIssue("settings panel cleanup", failure); }
        try { surface.cleanup().close(); } catch (Exception | LinkageError failure) { p.reportCompatibilityIssue("settings controls cleanup", failure); }
    }
    private void cleanupPartialRegistration() {
        if (partialRegistration == null) return;
        try { partialRegistration.deregister(); } catch (RuntimeException | LinkageError failure) { p.reportCompatibilityIssue("partial settings registration unload", failure); }
        partialRegistration = null;
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        cleanupPartialRegistration();
        if (nativeSurface != null) {
            cleanupNative(nativeSurface);
            nativeSurface = null;
        }
        cleanupMenu();
    }
}
