// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

import ninja.burpsuite.libs.generic.ImageHelper;
import ninja.burpsuite.libs.generic.WindowsAppUserModelId;
import ninja.burpsuite.libs.generic.WindowsIconResource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class BurpTitleAndIcon {

    // Common icon sizes so every OS taskbar and title bar can pick a sharp one.
    static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128};

    // Windows AppUserModelID used while a custom icon is active.
    // Used with an explicit relaunch ICO resource for installed launchers.
    static final String CUSTOM_WINDOWS_APP_ID = "ninja.burpsuite.sharpener.customicon";

    public static void resetTitle(BurpExtensionSharedParameters sharedParameters) {
        setTitle(sharedParameters, sharedParameters.get_originalBurpTitle());
    }

    public static void resetIcon(BurpExtensionSharedParameters sharedParameters) {
        long sequence = sharedParameters.iconUpdateSequence.incrementAndGet();
        SwingUtilities.invokeLater(() -> {
            if (sequence != sharedParameters.iconUpdateSequence.get()) return;
            sharedParameters.originalWindowIconImages.forEach((window, icons) -> {
                if (window.isDisplayable()) window.setIconImages(icons);
            });
            sharedParameters.originalWindowIconImages.clear();
            restoreOsTaskbarIcon(sharedParameters);
            sharedParameters.windowsTaskbarIconResource = null;
            sharedParameters.printDebugMessage("Burp Suite icon has been reset");
        });
        removeMainFrameWindowFocusListener(sharedParameters);
    }

    public static void setTitle(BurpExtensionSharedParameters sharedParameters, String title) {
        sharedParameters.get_mainFrameUsingMontoya().setTitle(title);
        sharedParameters.printDebugMessage("Burp Suite title was changed to: " + title);
    }

    public static void setTitle_noUiLock(BurpExtensionSharedParameters sharedParameters, String title) {
        SwingUtilities.invokeLater(() -> {
            setTitle(sharedParameters, title);
        });
    }

    // Builds a list of scaled copies of the image, one for each entry in ICON_SIZES.
    // Returns an empty list when the image is null.
    static List<Image> buildIconList(BufferedImage baseImage) {
        List<Image> icons = new ArrayList<>();
        if (baseImage == null) {
            return icons;
        }
        for (int size : ICON_SIZES) {
            Image scaled = ImageHelper.scaleImageToWidth(baseImage, size);
            if (scaled != null) {
                icons.add(scaled);
            }
        }
        return icons;
    }

    private static void setIcons(BurpExtensionSharedParameters sharedParameters, List<Image> icons) {
        if (icons == null || icons.isEmpty()) {
            return;
        }
        boolean anyWindowUpdated = false;
        for (Window window : Window.getWindows()) {
            // skip windows that already carry exactly these icons, so the focus
            // refresh is a no-op unless Burp rewrote them or a new window appeared
            if (!iconsAlreadyApplied(window.getIconImages(), icons)) {
                sharedParameters.originalWindowIconImages.putIfAbsent(window, new ArrayList<>(window.getIconImages()));
                window.setIconImages(icons);
                anyWindowUpdated = true;
            }
        }
        applyOsTaskbarIcon(sharedParameters, icons);
        if (anyWindowUpdated) {
            sharedParameters.printDebugMessage("Burp Suite icon has been updated");
        }
    }

    // True when the current icon list already holds exactly the desired images.
    // Compared by instance: the same list is reapplied for the lifetime of one
    // custom icon, and Window.getIconImages() returns the same Image references.
    static boolean iconsAlreadyApplied(List<Image> currentIcons, List<Image> desiredIcons) {
        if (currentIcons == null || currentIcons.size() != desiredIcons.size()) {
            return false;
        }
        for (int i = 0; i < desiredIcons.size(); i++) {
            if (currentIcons.get(i) != desiredIcons.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static void setIcons_noUiLock(BurpExtensionSharedParameters sharedParameters, List<Image> icons) {
        long sequence = sharedParameters.iconUpdateSequence.get();
        SwingUtilities.invokeLater(() -> {
            if (!sharedParameters.isUnloaded() && sequence == sharedParameters.iconUpdateSequence.get())
                setIcons(sharedParameters, icons);
        });
    }

    public static void setIcon(BurpExtensionSharedParameters sharedParameters, String imgPath, boolean isResource) {
        long sequence = sharedParameters.iconUpdateSequence.incrementAndGet();
        sharedParameters.backgroundTasks.submit("load application icon", () -> {
            BufferedImage baseImage;
            if (isResource) {
                baseImage = ImageHelper.loadImageResource(sharedParameters.extensionClass, imgPath);
            } else {
                baseImage = ImageHelper.loadImageFile(imgPath);
            }

            List<Image> iconList = buildIconList(baseImage);
            if (!iconList.isEmpty()) {
                String iconResource = null;
                if (WindowsAppUserModelId.isWindows()) {
                    try { iconResource = WindowsIconResource.cache(iconList); }
                    catch (Exception failure) { sharedParameters.reportCompatibilityIssue("Windows taskbar icon resource", failure); }
                }
                String resource = iconResource;
                SwingUtilities.invokeLater(() -> {
                    if (sharedParameters.isUnloaded() || sequence != sharedParameters.iconUpdateSequence.get()) return;
                    sharedParameters.windowsTaskbarIconResource = resource;
                    setIcons(sharedParameters, iconList);
                    installIconRefreshListener(sharedParameters, iconList);
                });
            } else {
                sharedParameters.printlnError("Image could not be loaded to be used as the Burp Suite icon: " + imgPath);
            }
        });
    }

    // Applies the icon to OS taskbars that ignore window icons:
    // the macOS Dock (java.awt.Taskbar) and the Windows taskbar (AppUserModelID).
    static void applyOsTaskbarIcon(BurpExtensionSharedParameters sharedParameters, List<Image> icons) {
        try {
            Image largestIcon = icons.get(icons.size() - 1);
            // the dock icon is process wide, not per window, so one successful call is
            // enough; this guard keeps the focus refresh away from the native API
            if (sharedParameters.appliedOsTaskbarIconImage != largestIcon
                    && !GraphicsEnvironment.isHeadless() && Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    if (!sharedParameters.originalTaskbarIconSaved) {
                        try {
                            sharedParameters.originalTaskbarIconImage = taskbar.getIconImage();
                        } catch (Exception e) {
                            sharedParameters.originalTaskbarIconImage = null;
                        }
                        sharedParameters.originalTaskbarIconSaved = true;
                    }
                    taskbar.setIconImage(largestIcon);
                    sharedParameters.appliedOsTaskbarIconImage = largestIcon;
                    sharedParameters.printDebugMessage("OS taskbar/dock icon has been updated");
                }
            }
        } catch (Exception | LinkageError e) {
            sharedParameters.printDebugMessage("Could not update the OS taskbar/dock icon: " + e.getMessage());
        }

        try {
            if (WindowsAppUserModelId.isWindows() && sharedParameters.windowsTaskbarIconResource != null) {
                var desired = new WindowsAppUserModelId.TaskbarProperties(CUSTOM_WINDOWS_APP_ID,
                        sharedParameters.windowsTaskbarIconResource);
                for (Frame frame : Frame.getFrames()) {
                    if (!frame.isDisplayable()) {
                        continue;
                    }
                    var original = WindowsAppUserModelId.getTaskbarProperties(frame);
                    if (desired.equals(original)) continue;
                    sharedParameters.originalWindowTaskbarProperties.putIfAbsent(frame, original);
                    if (!WindowsAppUserModelId.setTaskbarProperties(frame, desired)) {
                        throw new IllegalStateException("Windows rejected the taskbar icon properties");
                    }
                }
            }
        } catch (Exception | LinkageError e) {
            sharedParameters.reportCompatibilityIssue("Windows taskbar icon", e);
        }
    }

    static void restoreOsTaskbarIcon(BurpExtensionSharedParameters sharedParameters) {
        sharedParameters.appliedOsTaskbarIconImage = null;
        try {
            if (sharedParameters.originalTaskbarIconSaved && !GraphicsEnvironment.isHeadless() && Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE) && sharedParameters.originalTaskbarIconImage != null) {
                    taskbar.setIconImage(sharedParameters.originalTaskbarIconImage);
                }
                sharedParameters.originalTaskbarIconImage = null;
                sharedParameters.originalTaskbarIconSaved = false;
            }
        } catch (Exception | LinkageError e) {
            sharedParameters.printDebugMessage("Could not restore the OS taskbar/dock icon: " + e.getMessage());
        }

        try {
            if (WindowsAppUserModelId.isWindows()) {
                synchronized (sharedParameters.originalWindowTaskbarProperties) {
                    for (var entry : sharedParameters.originalWindowTaskbarProperties.entrySet()) {
                        Window window = entry.getKey();
                        if (window == null || !window.isDisplayable()) {
                            continue;
                        }
                        if (!WindowsAppUserModelId.setTaskbarProperties(window, entry.getValue()))
                            sharedParameters.printlnError("Could not restore a Windows taskbar icon property");
                    }
                    sharedParameters.originalWindowTaskbarProperties.clear();
                }
            }
        } catch (Exception | LinkageError e) {
            sharedParameters.printDebugMessage("Could not restore the Windows AppUserModelID: " + e.getMessage());
        }
    }

    // Burp can rewrite window icons, so the icon is applied again on focus changes.
    // Both focus events are used because new or detached windows can appear at any
    // time, but setIcons skips windows that already have the icon, so a refresh
    // that finds nothing to do costs no Swing or native calls.
    // The listener instance is stored so unload can remove exactly this listener,
    // even when Burp or another extension adds its own focus listener later.
    static void installIconRefreshListener(BurpExtensionSharedParameters sharedParameters, List<Image> iconList) {
        removeMainFrameWindowFocusListener(sharedParameters);
        Window frame = sharedParameters.get_mainFrameUsingMontoya();
        if (frame == null) return;

        WindowFocusListener mainFrameWindowFocusListener = new WindowFocusListener() {

            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (sharedParameters.iconRefreshWindowFocusListener == this)
                    setIcons_noUiLock(sharedParameters, iconList);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (sharedParameters.iconRefreshWindowFocusListener == this)
                    setIcons_noUiLock(sharedParameters, iconList);
            }
        };

        frame.addWindowFocusListener(mainFrameWindowFocusListener);
        sharedParameters.iconRefreshWindow = frame;
        sharedParameters.iconRefreshWindowFocusListener = mainFrameWindowFocusListener;
    }

    static void removeMainFrameWindowFocusListener(BurpExtensionSharedParameters sharedParameters) {
        WindowFocusListener listener = sharedParameters.iconRefreshWindowFocusListener;
        if (listener != null) {
            sharedParameters.iconRefreshWindowFocusListener = null;
            if (sharedParameters.iconRefreshWindow != null)
                sharedParameters.iconRefreshWindow.removeWindowFocusListener(listener);
            sharedParameters.iconRefreshWindow = null;
        }
    }
}
