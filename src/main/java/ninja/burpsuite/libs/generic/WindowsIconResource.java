// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** PNG-backed Windows ICO resources, using JDK codecs only. No native calls here. */
public final class WindowsIconResource {
    private WindowsIconResource() {}

    static byte[] encode(List<Image> images) throws IOException {
        if (images == null || images.isEmpty() || images.size() > 16)
            throw new IllegalArgumentException("Expected 1 to 16 icon sizes");
        List<byte[]> pngs = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        for (Image image : images) {
            if (image == null) throw new IllegalArgumentException("Null icon image");
            ImageIcon loaded = new ImageIcon(image);
            int size = loaded.getIconWidth();
            if (size < 1 || size > 256 || loaded.getIconHeight() < 1)
                throw new IllegalArgumentException("ICO width must be 1 to 256 pixels");
            BufferedImage buffered = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            var graphics = buffered.createGraphics();
            try {
                double scale = size / (double)Math.max(size, loaded.getIconHeight());
                int width = Math.max(1, (int)Math.round(size * scale));
                int height = Math.max(1, (int)Math.round(loaded.getIconHeight() * scale));
                graphics.drawImage(image, (size-width)/2, (size-height)/2, width, height, null);
            }
            finally { graphics.dispose(); }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            if (!ImageIO.write(buffered, "png", png)) throw new IOException("PNG codec unavailable");
            pngs.add(png.toByteArray()); sizes.add(size);
        }
        int offset = 6 + 16 * pngs.size();
        int total = offset + pngs.stream().mapToInt(png -> png.length).sum();
        ByteBuffer ico = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short)0).putShort((short)1).putShort((short)pngs.size());
        for (int i = 0; i < pngs.size(); i++) {
            int size = sizes.get(i);
            ico.put((byte)(size == 256 ? 0 : size)).put((byte)(size == 256 ? 0 : size));
            ico.put((byte)0).put((byte)0).putShort((short)1).putShort((short)32);
            ico.putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        pngs.forEach(ico::put);
        return ico.array();
    }

    public static String cache(List<Image> images) throws IOException {
        return cache(images, Path.of(System.getProperty("user.home"), ".burp-sharpener", "icons"));
    }

    static String cache(List<Image> images, Path directory) throws IOException {
        byte[] ico = encode(images);
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ico)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        // Persistent resources keep user-created pinned shortcuts valid after unload.
        Files.createDirectories(directory);
        Path target = directory.resolve(hash + ".ico");
        if (!Files.isRegularFile(target) || Files.size(target) != ico.length
                || !java.util.Arrays.equals(Files.readAllBytes(target), ico)) {
            Path staged = Files.createTempFile(directory, "icon-", ".tmp");
            try {
                Files.write(staged, ico);
                try { Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(staged); }
        }
        return target.toAbsolutePath() + ",0";
    }
}
