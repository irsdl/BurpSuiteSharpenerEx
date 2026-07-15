// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

import ninja.burpsuite.libs.generic.ImageHelper;
import ninja.burpsuite.libs.generic.WindowsAppUserModelId;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BurpTitleAndIcon {

    // Common icon sizes so every OS taskbar and title bar can pick a sharp one.
    static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128};

    // Windows AppUserModelID used while a custom icon is active.
    // It detaches Burp windows from the native launcher, so the taskbar
    // uses the window icon instead of the launcher exe icon.
    static final String CUSTOM_WINDOWS_APP_ID = "ninja.burpsuite.sharpener.customicon";

    public static void resetTitle(BurpExtensionSharedParameters sharedParameters) {
        setTitle(sharedParameters, sharedParameters.get_originalBurpTitle());
    }

    public static void resetIcon(BurpExtensionSharedParameters sharedParameters) {
        SwingUtilities.invokeLater(() -> {
            List<Image> originalIcons = sharedParameters.get_originalBurpIcons();
            if (originalIcons != null && !originalIcons.isEmpty()) {
                for (Window window : Window.getWindows()) {
                    window.setIconImages(originalIcons);
                }
            }
            restoreOsTaskbarIcon(sharedParameters);
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
        for (Window window : Window.getWindows()) {
            window.setIconImages(icons);
        }
        applyOsTaskbarIcon(sharedParameters, icons);
        sharedParameters.printDebugMessage("Burp Suite icon has been updated");
    }

    private static void setIcons_noUiLock(BurpExtensionSharedParameters sharedParameters, List<Image> icons) {
        SwingUtilities.invokeLater(() -> {
            setIcons(sharedParameters, icons);
        });
    }

    public static void setIcon(BurpExtensionSharedParameters sharedParameters, String imgPath, boolean isResource) {
        BufferedImage baseImage;
        if (isResource) {
            baseImage = ImageHelper.loadImageResource(sharedParameters.extensionClass, imgPath);
        } else {
            baseImage = ImageHelper.loadImageFile(imgPath);
        }

        List<Image> iconList = buildIconList(baseImage);
        if (!iconList.isEmpty()) {
            setIcons_noUiLock(sharedParameters, iconList);
            installIconRefreshListener(sharedParameters, iconList);
        } else {
            sharedParameters.printlnError("Image could not be loaded to be used as the Burp Suite icon: " + imgPath);
        }
    }

    // Applies the icon to OS taskbars that ignore window icons:
    // the macOS Dock (java.awt.Taskbar) and the Windows taskbar (AppUserModelID).
    private static void applyOsTaskbarIcon(BurpExtensionSharedParameters sharedParameters, List<Image> icons) {
        try {
            if (!GraphicsEnvironment.isHeadless() && Taskbar.isTaskbarSupported()) {
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
                    taskbar.setIconImage(icons.get(icons.size() - 1)); // the largest one
                    sharedParameters.printDebugMessage("OS taskbar/dock icon has been updated");
                }
            }
        } catch (Exception | LinkageError e) {
            sharedParameters.printDebugMessage("Could not update the OS taskbar/dock icon: " + e.getMessage());
        }

        try {
            if (WindowsAppUserModelId.isWindows()) {
                for (Frame frame : Frame.getFrames()) {
                    if (!frame.isDisplayable() || sharedParameters.originalWindowAppIds.containsKey(frame)) {
                        continue;
                    }
                    String originalAppId = WindowsAppUserModelId.getAppUserModelId(frame);
                    if (WindowsAppUserModelId.setAppUserModelId(frame, CUSTOM_WINDOWS_APP_ID)) {
                        sharedParameters.originalWindowAppIds.put(frame, originalAppId == null ? "" : originalAppId);
                        sharedParameters.printDebugMessage("Windows AppUserModelID has been set on: " + frame.getTitle());
                    }
                }
            }
        } catch (Exception | LinkageError e) {
            sharedParameters.printDebugMessage("Could not update the Windows AppUserModelID: " + e.getMessage());
        }
    }

    private static void restoreOsTaskbarIcon(BurpExtensionSharedParameters sharedParameters) {
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
                synchronized (sharedParameters.originalWindowAppIds) {
                    for (Map.Entry<Window, String> entry : sharedParameters.originalWindowAppIds.entrySet()) {
                        Window window = entry.getKey();
                        if (window == null || !window.isDisplayable()) {
                            continue;
                        }
                        String originalAppId = entry.getValue();
                        WindowsAppUserModelId.setAppUserModelId(window, originalAppId.isEmpty() ? null : originalAppId);
                    }
                    sharedParameters.originalWindowAppIds.clear();
                }
            }
        } catch (Exception | LinkageError e) {
            sharedParameters.printDebugMessage("Could not restore the Windows AppUserModelID: " + e.getMessage());
        }
    }

    // Burp can rewrite window icons, so the icon is applied again on focus changes.
    // The listener instance is stored so unload can remove exactly this listener,
    // even when Burp or another extension adds its own focus listener later.
    static void installIconRefreshListener(BurpExtensionSharedParameters sharedParameters, List<Image> iconList) {
        removeMainFrameWindowFocusListener(sharedParameters);

        WindowFocusListener mainFrameWindowFocusListener = new WindowFocusListener() {

            @Override
            public void windowGainedFocus(WindowEvent e) {
                setIcons_noUiLock(sharedParameters, iconList);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                setIcons_noUiLock(sharedParameters, iconList);
            }
        };

        sharedParameters.get_mainFrameUsingMontoya().addWindowFocusListener(mainFrameWindowFocusListener);
        sharedParameters.iconRefreshWindowFocusListener = mainFrameWindowFocusListener;
    }

    static void removeMainFrameWindowFocusListener(BurpExtensionSharedParameters sharedParameters) {
        WindowFocusListener listener = sharedParameters.iconRefreshWindowFocusListener;
        if (listener != null) {
            sharedParameters.iconRefreshWindowFocusListener = null;
            sharedParameters.get_mainFrameUsingMontoya().removeWindowFocusListener(listener);
        }
    }
}
