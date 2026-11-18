// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.*;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WindowsIconResourceTest {
    @TempDir Path temporary;

    @Test void directoryEntriesPointAtReadablePngImagesOfTheRightSizes() throws Exception {
        byte[] ico = WindowsIconResource.encode(List.of(image(16), image(32), image(256)));
        ByteBuffer directory = ByteBuffer.wrap(ico).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, directory.getShort()); assertEquals(1, directory.getShort()); assertEquals(3, directory.getShort());
        int end = 6 + 3 * 16;
        for (int size : new int[]{16,32,256}) {
            assertEquals(size == 256 ? 0 : size, Byte.toUnsignedInt(directory.get()));
            assertEquals(size == 256 ? 0 : size, Byte.toUnsignedInt(directory.get()));
            assertEquals(0,directory.get()); assertEquals(0,directory.get());
            assertEquals(1,directory.getShort()); assertEquals(32,directory.getShort());
            int length = directory.getInt(), offset = directory.getInt();
            assertEquals(end, offset);
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(ico, offset, length));
            assertEquals(size, decoded.getWidth()); assertEquals(size, decoded.getHeight());
            assertEquals(0xff1278ab, decoded.getRGB(0,0));
            end = offset + length;
        }
        assertEquals(ico.length, end);
    }

    @Test void rejectsEmptyNullAndOversizedImages() {
        assertThrows(IllegalArgumentException.class, () -> WindowsIconResource.encode(null));
        assertThrows(IllegalArgumentException.class, () -> WindowsIconResource.encode(List.of()));
        assertThrows(IllegalArgumentException.class, () -> WindowsIconResource.encode(java.util.Collections.singletonList(null)));
        assertThrows(IllegalArgumentException.class, () -> WindowsIconResource.encode(List.of(image(257))));
    }

    @Test void portraitIconsAreFittedInsideTransparentSquareResources() throws Exception {
        BufferedImage portrait = new BufferedImage(16,32,BufferedImage.TYPE_INT_ARGB);
        var graphics = portrait.createGraphics();
        try { graphics.setColor(java.awt.Color.RED); graphics.fillRect(0,0,16,32); }
        finally { graphics.dispose(); }
        byte[] ico = WindowsIconResource.encode(List.of(portrait));
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(ico,22,ico.length-22));
        assertEquals(16,decoded.getWidth()); assertEquals(16,decoded.getHeight());
        assertEquals(0,decoded.getRGB(0,0));
        assertEquals(java.awt.Color.RED.getRGB(),decoded.getRGB(8,8));
    }

    @Test void cacheReusesIdenticalIconsAndRepairsCorruptContent() throws Exception {
        List<Image> icons = List.of(image(16));
        String resource = WindowsIconResource.cache(icons, temporary.resolve("icons with spaces"));
        assertTrue(resource.endsWith(".ico,0"));
        Path icon = Path.of(resource.substring(0, resource.length()-2));
        byte[] expected = Files.readAllBytes(icon);
        assertEquals(resource, WindowsIconResource.cache(icons, icon.getParent()));
        Files.writeString(icon,"corrupt");
        assertEquals(resource, WindowsIconResource.cache(icons, icon.getParent()));
        assertArrayEquals(expected, Files.readAllBytes(icon));
        try (var contents = Files.list(icon.getParent())) { assertEquals(1,contents.count()); }
    }

    private static BufferedImage image(int size) {
        BufferedImage image = new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,0xff1278ab);
        return image;
    }
}
