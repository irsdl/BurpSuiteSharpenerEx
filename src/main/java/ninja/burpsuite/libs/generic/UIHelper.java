// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

public class UIHelper {

    // Show a message to the user
    public static void showMessage(final String strMsg, final String strTitle, Component parentCmp) {
        new Thread(() -> JOptionPane.showMessageDialog(parentCmp, strMsg, strTitle, JOptionPane.INFORMATION_MESSAGE)).start();

    }

    // Show a message to the user
    public static void showWarningMessage(final String strMsg, Component parentCmp) {
        new Thread(() -> JOptionPane.showMessageDialog(parentCmp, strMsg, "Warning", JOptionPane.WARNING_MESSAGE)).start();
    }

    // Show a message to the user
    public static String showPlainInputMessage(final String strMessage, final String strTitle, final String defaultValue, Component parentCmp) {
        String output = (String) JOptionPane.showInputDialog(parentCmp,
                strMessage, strTitle, JOptionPane.PLAIN_MESSAGE, null, null, defaultValue);
        if (output == null) {
            output = defaultValue;
        }

        if (output == null)
            output = "";

        return output;
    }

    // Common method to ask a multiline question
    public static String[] showPlainInputMessages(final String[] strMessages, final String strTitle, final String[] defaultValues, Component parentCmp) {
        String[] output = new String[strMessages.length];
        java.util.List<Object> strMessagesObjectList = new ArrayList<>();

        for (int i = 0; i < strMessages.length; i++) {
            String defaultValue = "";
            if (defaultValues.length > i)
                defaultValue = defaultValues[i];
            strMessagesObjectList.add(strMessages[i]);
            strMessagesObjectList.add(new JTextField(defaultValue));
        }

        int option = JOptionPane.showConfirmDialog(parentCmp, strMessagesObjectList.toArray(), strTitle, JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            for (int i = 0; i < strMessages.length; i++) {
                output[i] = ((JTextField) strMessagesObjectList.get(i * 2 + 1)).getText();
            }
        }
        return output;
    }

    // Common method to ask a multiple question
    public static Integer askConfirmMessage(final String strTitle, final String strQuestion, String[] msgOptions, Component parentCmp) {
        Integer output = JOptionPane.showOptionDialog(parentCmp,
                strQuestion,
                strTitle,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                msgOptions,
                msgOptions[0]);
        // showOptionDialog returns an int, so this can never be null
        // a closed dialog already returns JOptionPane.CLOSED_OPTION which is -1
        return output;
    }

    // to update the JCheckbox background colour after using the customizeUiComponent() method
    public static void updateJCheckBoxBackground(Container c) {
        Component[] components = c.getComponents();
        for (Component com : components) {
            if (com instanceof JCheckBox) {
                com.setBackground(c.getBackground());
            } else if (com instanceof Container) {
                updateJCheckBoxBackground((Container) com);
            }
        }
    }

    // Show directory dialog and return the path
    public static String showDirectoryDialog(final String initialPath, Component parentCmp) {
        return showFileDialog(initialPath, true, null, parentCmp, false);
    }

    // Show directory dialog and return the path
    public static String showDirectorySaveDialog(final String initialPath, Component parentCmp) {
        return showFileDialog(initialPath, true, null, parentCmp, true);
    }

    // Show file dialog and return the file path
    public static String showFileDialog(final String initialPath, FileFilter fileFilter, Component parentCmp) {
        return showFileDialog(initialPath, false, fileFilter, parentCmp, false);
    }

    // Show file chooser
    public static String showFileDialog(final String initialPath, final boolean dirOnly, FileFilter fileFilter, Component parentCmp, boolean isSave) {
        String filePath = "";
        JFileChooser _fileChooser = new JFileChooser();
        if (dirOnly)
            _fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fileFilter != null)
            _fileChooser.setFileFilter(fileFilter);

        if (!initialPath.trim().isEmpty()) {
            File file = new File(initialPath);
            _fileChooser.setCurrentDirectory(file);
        }

        int returnVal;
        if (isSave) {
            returnVal = _fileChooser.showSaveDialog(parentCmp);
        } else {
            returnVal = _fileChooser.showOpenDialog(parentCmp);
        }

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePath = _fileChooser.getSelectedFile().getAbsolutePath();
        }

        if (filePath == null)
            filePath = "";

        return filePath;
    }

    // Combined bounds of all screens, or null when they cannot be read.
    public static Rectangle getScreenUnionBounds() {
        try {
            Rectangle bounds = null;
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (GraphicsDevice gd : ge.getScreenDevices()) {
                Rectangle screenBounds = gd.getDefaultConfiguration().getBounds();
                if (bounds == null) {
                    bounds = new Rectangle(screenBounds);
                } else {
                    bounds.add(screenBounds);
                }
            }
            return bounds;
        } catch (Exception e) {
            System.err.println("Error in getScreenUnionBounds, it has been ignored");
            return null;
        }
    }

    public static boolean isFrameOutOffScreen(JFrame jframe, double offScreenMargin) {
        boolean result = false;
        try {
            Rectangle screenUnion = getScreenUnionBounds();
            if (screenUnion != null) {
                result = isBoundsOutOfScreen(jframe.getBounds(), screenUnion, offScreenMargin);
            }
        } catch (Exception e) {
            System.err.println("Error in isFrameOutOffScreen, it has been ignored");
        }
        return result;
    }

    // True when frameBounds is more than offScreenMargin (0 to 1, fraction of the
    // frame size) outside the combined screen area. Pure logic so it can be tested headless.
    public static boolean isBoundsOutOfScreen(Rectangle frameBounds, Rectangle screenUnion, double offScreenMargin) {
        if (offScreenMargin > 1 || offScreenMargin < 0)
            offScreenMargin = 0;

        double widthOffset = offScreenMargin * frameBounds.getWidth();
        double heightOffset = offScreenMargin * frameBounds.getHeight();
        Rectangle boundsWithThreshold = new Rectangle((int) (screenUnion.getX() - widthOffset),
                (int) (screenUnion.getY() - heightOffset),
                (int) (screenUnion.getWidth() + 2 * widthOffset),
                (int) (screenUnion.getHeight() + 2 * heightOffset)
        );

        return !boundsWithThreshold.contains(frameBounds);
    }

    // True when the size is missing or smaller than minSize in either direction.
    public static boolean isSizeTooSmall(Dimension size, Dimension minSize) {
        return size == null || size.width < minSize.width || size.height < minSize.height;
    }

    public static void moveFrameToCenter(JFrame jframe) {
        try {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            Rectangle usableBounds = new Rectangle(screenBounds.x + insets.left,
                    screenBounds.y + insets.top,
                    screenBounds.width - insets.left - insets.right,
                    screenBounds.height - insets.top - insets.bottom);
            jframe.setLocation(getCenteredLocation(usableBounds, jframe.getSize()));
        } catch (Exception e) {
            System.err.println("Error in moveFrameToCenter, it has been ignored");
        }

    }

    // Centered top-left location for a frame inside the usable screen area.
    // Never above or left of the usable area, so the title bar stays reachable
    // even when the frame is larger than the screen. Pure logic so it can be tested headless.
    public static Point getCenteredLocation(Rectangle usableScreenBounds, Dimension frameSize) {
        int x = usableScreenBounds.x + (usableScreenBounds.width - frameSize.width) / 2;
        int y = usableScreenBounds.y + (usableScreenBounds.height - frameSize.height) / 2;
        return new Point(Math.max(x, usableScreenBounds.x), Math.max(y, usableScreenBounds.y));
    }

    public static void simulateClick(JLabel label) {
        Point labelLocation = label.getLocationOnScreen();
        int clickX = labelLocation.x + label.getWidth() / 2;
        int clickY = labelLocation.y + label.getHeight() / 2;

        MouseEvent pressEvent = new MouseEvent(label, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, clickX, clickY, 1, false);
        MouseEvent releaseEvent = new MouseEvent(label, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, clickX, clickY, 1, false);
        MouseEvent clickedEvent = new MouseEvent(label, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, clickX, clickY, 1, false);
        /*
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(pressEvent);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(releaseEvent);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(clickedEvent);
        */
        label.dispatchEvent(pressEvent);
        label.dispatchEvent(releaseEvent);
        label.dispatchEvent(clickedEvent);

    }

}
