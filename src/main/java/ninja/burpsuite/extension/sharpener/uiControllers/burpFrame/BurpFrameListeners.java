// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.burpFrame;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.uiControllers.shortcuts.ShortcutMappings;
import ninja.burpsuite.libs.generic.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class BurpFrameListeners implements ComponentListener {
    private final ExtensionSharedParameters sharedParameters;

    // A frame smaller than this is treated as practically invisible and is restored.
    public static final Dimension MIN_VISIBLE_FRAME_SIZE = new Dimension(100, 100);

    private Lock recenterLock = new ReentrantLock();
    private Lock resizedFrameLock = new ReentrantLock();
    private Lock movedFrameLock = new ReentrantLock();
    private volatile boolean isRecenterInProgress = false;
    private volatile boolean isResizedFrameCheckInProgress = false;
    private volatile boolean isMovedFrameCheckInProgress = false;

    public BurpFrameListeners(ExtensionSharedParameters sharedParameters) {
        this.sharedParameters = sharedParameters;
        addBurpFrameListener(sharedParameters.get_mainFrameUsingMontoya());
        boolean detectOffScreenPosition = sharedParameters.preferences.safeGetBooleanSetting("detectOffScreenPosition");
        if (detectOffScreenPosition && !isRecenterInProgress) {
            checkAndCenterOffScreen(sharedParameters.get_mainFrameUsingMontoya(), 0.1, true);
        }
    }

    public void addBurpFrameListener(JFrame jframe) {
        sharedParameters.printDebugMessage("addBurpFrameListener");
        try {
            jframe.addComponentListener(this);
            installShortcuts(jframe);
        } catch (Exception e) {
            sharedParameters.printDebugMessage("Error in BurpFrameListeners.addBurpFrameListener");
        }

    }

    // Installs the Burp frame shortcuts from the registry, honouring user overrides.
    public void installShortcuts(JFrame jframe) {
        HashMap<String, Consumer<ActionEvent>> handlers = new HashMap<>();
        handlers.put(ShortcutMappings.ACTION_KEY_PREFIX + "MoveToCenter", e -> UIHelper.moveFrameToCenter(jframe));
        ShortcutMappings.installOnBurpFrame(jframe.getRootPane(),
                ShortcutMappings.getSavedOverrides(sharedParameters), handlers);
    }

    // Reinstalls the key bindings so a shortcut change is applied without a full reload.
    public void reloadShortcuts(JFrame jframe) {
        try {
            installShortcuts(jframe);
        } catch (Exception e) {
            sharedParameters.printDebugMessage("Error in BurpFrameListeners.reloadShortcuts");
        }
    }

    public void removeBurpFrameListener(JFrame jframe) {
        sharedParameters.printDebugMessage("removeBurpFrameListener");
        try {
            jframe.removeComponentListener(this);
            // removes all Sharpener key bindings from the main window
            ShortcutMappings.uninstallFromComponent(jframe.getRootPane());
        } catch (Exception e) {
            sharedParameters.printDebugMessage("Error in BurpFrameListeners.removeBurpFrameListener");
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
        if (!isResizedFrameCheckInProgress) {
            try {
                if (resizedFrameLock == null)
                    resizedFrameLock = new ReentrantLock();

                if (resizedFrameLock.tryLock(5, TimeUnit.SECONDS)) {
                    try {
                        isResizedFrameCheckInProgress = true;
                        sharedParameters.delayedTasks.schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        if (sharedParameters.isUnloaded()) {
                                            isResizedFrameCheckInProgress = false;
                                            return;
                                        }
                                        try {
                                            saveBoundsAndCheckOffScreen();
                                        } catch (Exception e) {
                                            sharedParameters.printDebugMessage("Error in BurpFrameListeners.componentResized");
                                        } finally {
                                            isResizedFrameCheckInProgress = false;

                                        }
                                    }
                                },
                                2000 // 2 seconds delay to decrease the amount of checking process
                        );
                    } finally {
                        resizedFrameLock.unlock();
                    }
                }
            } catch (Exception err) {
                isResizedFrameCheckInProgress = false;
            }
        }
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        if (!isMovedFrameCheckInProgress) {
            try {
                if (movedFrameLock == null)
                    movedFrameLock = new ReentrantLock();

                if (movedFrameLock.tryLock(5, TimeUnit.SECONDS)) {
                    try {
                        isMovedFrameCheckInProgress = true;
                        sharedParameters.delayedTasks.schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        if (sharedParameters.isUnloaded()) {
                                            isMovedFrameCheckInProgress = false;
                                            return;
                                        }
                                        try {
                                            saveBoundsAndCheckOffScreen();
                                        } catch (Exception e) {
                                            sharedParameters.printDebugMessage("Error in BurpFrameListeners.componentMoved");
                                        } finally {
                                            isMovedFrameCheckInProgress = false;
                                        }

                                    }
                                },
                                1000 // 1 second delay to decrease the amount of checking process
                        );
                    } finally {
                        movedFrameLock.unlock();
                    }
                }
            } catch (Exception err) {
                isMovedFrameCheckInProgress = false;
            }
        }
    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }

    public void checkAndCenterOffScreen(JFrame jframe, double offScreenMargin, boolean isChoice) {
        if (!isRecenterInProgress) {
            try {
                if (recenterLock == null)
                    recenterLock = new ReentrantLock();

                if (recenterLock.tryLock(5, TimeUnit.SECONDS)) {
                    try {
                        isRecenterInProgress = true;
                        sharedParameters.delayedTasks.schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        // no exception may escape: it would kill the shared timer thread
                                        try {
                                            if (sharedParameters.isUnloaded()) {
                                                isRecenterInProgress = false;
                                                return;
                                            }
                                            // UI work belongs on the EDT; it also keeps a modal
                                            // dialog from blocking the shared timer thread
                                            SwingUtilities.invokeLater(() -> {
                                                try {
                                                    restoreFrameIfNotVisible(jframe, offScreenMargin, isChoice);
                                                } catch (Exception e) {
                                                    sharedParameters.printDebugMessage("Error in BurpFrameListeners.checkAndCenterOffScreen");
                                                } finally {
                                                    isRecenterInProgress = false;
                                                }
                                            });
                                        } catch (Exception e) {
                                            isRecenterInProgress = false;
                                        }
                                    }
                                },
                                1000 // 1 second delay to decrease the amount of checking process
                        );
                    } finally {
                        recenterLock.unlock();
                    }
                }
            } catch (Exception err) {
                isRecenterInProgress = false;
            }
        }
    }

    // Saves the current window bounds and starts the off screen check when enabled.
    // Runs from the delayed move and resize tasks.
    private void saveBoundsAndCheckOffScreen() {
        JFrame jframe = sharedParameters.get_mainFrameUsingMontoya();
        if (!hasUsableBounds(jframe))
            return;

        Rectangle frameBounds = jframe.getBounds();
        sharedParameters.preferences.safeSetSetting("lastApplicationSize", frameBounds.getSize(), Preferences.Visibility.GLOBAL);
        sharedParameters.preferences.safeSetSetting("lastApplicationPosition", frameBounds.getLocation(), Preferences.Visibility.GLOBAL);
        boolean detectOffScreenPosition = sharedParameters.preferences.safeGetBooleanSetting("detectOffScreenPosition");
        if (detectOffScreenPosition && !isRecenterInProgress) {
            checkAndCenterOffScreen(jframe, 0.8, false);
        }
    }

    // A minimised or hidden frame reports bogus bounds (Windows uses -32000,-32000),
    // so it must not be saved or recentered.
    private boolean hasUsableBounds(JFrame jframe) {
        return jframe != null && jframe.isShowing() && (jframe.getExtendedState() & Frame.ICONIFIED) == 0;
    }

    // Runs on the EDT. Restores the frame when it is off screen or too small to be seen.
    private void restoreFrameIfNotVisible(JFrame jframe, double offScreenMargin, boolean isChoice) {
        if (sharedParameters.isUnloaded() || !hasUsableBounds(jframe))
            return;

        boolean tooSmall = UIHelper.isSizeTooSmall(jframe.getSize(), MIN_VISIBLE_FRAME_SIZE);
        boolean offScreen = UIHelper.isFrameOutOffScreen(jframe, offScreenMargin);
        if (!tooSmall && !offScreen)
            return;

        String problem = tooSmall ? "too small to be seen"
                : "at least " + (int) (offScreenMargin * 100) + "% outside the screen";

        if (isChoice) {
            int response = UIHelper.askConfirmMessage(sharedParameters.extensionName + ": Invisible Burp Window",
                    "The Burp Suite window is " + problem + ", do you want to restore it to the center of the screen?",
                    new String[]{"Yes", "No"}, null);
            if (response != 0)
                return;
        } else {
            UIHelper.showWarningMessage(sharedParameters.extensionName + ": The Burp Suite window was " + problem +
                    ", so it has been restored to the center of the screen.", null);
        }

        if (tooSmall) {
            jframe.setSize(getDefaultFrameSize());
        }
        UIHelper.moveFrameToCenter(jframe);
    }

    // A sane fallback window size: two thirds of the primary screen.
    private Dimension getDefaultFrameSize() {
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            return new Dimension(screenSize.width * 2 / 3, screenSize.height * 2 / 3);
        } catch (Exception e) {
            return new Dimension(800, 600);
        }
    }

}
