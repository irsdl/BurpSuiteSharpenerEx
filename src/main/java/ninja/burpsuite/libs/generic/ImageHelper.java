// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

public class ImageHelper {
    public static Image scaleImageToWidth(BufferedImage image, int width) {
        if (image == null)
            return null;

        int height = (int) (Math.floor((image.getHeight() * width) / (double) image.getWidth()));
        if (image.getWidth() > width) {
            return image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        } else {
            return image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        }
    }

    public static Image scaleImageToWidth2(BufferedImage image, int width) {
        if (image == null)
            return null;

        int height = (int) (Math.floor((image.getHeight() * width) / (double) image.getWidth()));
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, width, height, null);
        graphics2D.dispose();
        return resizedImage;
    }

    public static BufferedImage loadImageResource(String filename) {
        return loadImageResource(UIHelper.class, filename);
    }

    public static BufferedImage loadImageResource(Class claz, String filePath) {
        URL imageURLMain = null;

        if (!filePath.startsWith("/")) {
            imageURLMain = claz.getResource("/" + filePath);
        }

        if (imageURLMain == null) {
            imageURLMain = claz.getResource(filePath);
        }

        if (imageURLMain != null) {
            Image original = new ImageIcon(imageURLMain).getImage();
            ImageIcon originalIcon = new ImageIcon(original);
            BufferedImage bufferedImage = new BufferedImage(originalIcon.getIconWidth(), originalIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(originalIcon.getImage(), null, null);
            return bufferedImage;
        }
        return null;
    }

    public static BufferedImage loadImageFile(String filePath) {
        BufferedImage bufferedImage = null;
        File file = new File(filePath);

        if (file.exists() && file.isFile()) {
            try {
                Image original = new ImageIcon(filePath).getImage();
                ImageIcon originalIcon = new ImageIcon(original);
                bufferedImage = new BufferedImage(originalIcon.getIconWidth(), originalIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(originalIcon.getImage(), null, null);
            } catch (Exception e) {
                System.err.println(e.getMessage() + "\r\n" + getStackTrace(e));
            }
        }
        return bufferedImage;
    }

    // Paints a component into an image without using the screen. Unlike an OS level
    // screen capture, this also works when the session is locked, so it is useful
    // for automated UI checks over a debugger.
    public static BufferedImage captureComponent(Component component) {
        if (component == null || component.getWidth() <= 0 || component.getHeight() <= 0)
            return null;

        BufferedImage bufferedImage = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        component.paint(graphics2D);
        graphics2D.dispose();
        return bufferedImage;
    }

    // Captures a component and saves it as a PNG file. Returns true on success.
    // This is one expression, so it can also be called from a debugger such as jdb.
    public static boolean saveComponentAsPng(Component component, String filePath) {
        try {
            BufferedImage bufferedImage = captureComponent(component);
            if (bufferedImage == null)
                return false;

            return ImageIO.write(bufferedImage, "png", new File(filePath));
        } catch (Exception e) {
            System.err.println(e.getMessage() + "\r\n" + getStackTrace(e));
            return false;
        }
    }

    // https://alvinalexander.com/java/java-copy-image-to-clipboard-example/
    // code below from exampledepot.com
    //This method writes an image to the system clipboard.
    //otherwise it returns null.
    public static void setClipboard(Image image) {
        ImageSelection imgSel = new ImageSelection(image);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
    }


    // This class is used to hold an image while on the clipboard.
    static class ImageSelection implements Transferable {
        private final Image image;

        public ImageSelection(Image image) {
            this.image = image;
        }

        // Returns supported flavors
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        // Returns true if flavor is supported
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        // Returns image
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
