// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BurpTitleAndIconTest {

    private static BufferedImage squareImage(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, size, size);
        graphics.dispose();
        return image;
    }

    // ImageIcon waits for the image to be fully loaded, so the size is reliable
    private static int loadedWidth(Image image) {
        return new ImageIcon(image).getIconWidth();
    }

    @Test
    void buildIconListReturnsOneIconPerSize() {
        List<Image> icons = BurpTitleAndIcon.buildIconList(squareImage(256));
        assertEquals(BurpTitleAndIcon.ICON_SIZES.length, icons.size());
        for (int i = 0; i < icons.size(); i++) {
            assertEquals(BurpTitleAndIcon.ICON_SIZES[i], loadedWidth(icons.get(i)));
        }
    }

    @Test
    void buildIconListUpscalesSmallImages() {
        List<Image> icons = BurpTitleAndIcon.buildIconList(squareImage(8));
        assertEquals(BurpTitleAndIcon.ICON_SIZES.length, icons.size());
        for (int i = 0; i < icons.size(); i++) {
            assertEquals(BurpTitleAndIcon.ICON_SIZES[i], loadedWidth(icons.get(i)));
        }
    }

    @Test
    void buildIconListReturnsEmptyListForNullImage() {
        assertTrue(BurpTitleAndIcon.buildIconList(null).isEmpty());
    }

    @Test
    void iconSizesAreAscendingSoLastIsTheLargest() {
        // applyOsTaskbarIcon uses the last list entry as the largest icon
        for (int i = 1; i < BurpTitleAndIcon.ICON_SIZES.length; i++) {
            assertTrue(BurpTitleAndIcon.ICON_SIZES[i] > BurpTitleAndIcon.ICON_SIZES[i - 1]);
        }
    }

    @Test
    void customWindowsAppIdIsStable() {
        // The taskbar groups windows by this value, so it must not change between releases
        assertEquals("ninja.burpsuite.sharpener.customicon", BurpTitleAndIcon.CUSTOM_WINDOWS_APP_ID);
    }

    @Test
    void iconsAlreadyAppliedIsTrueForTheSameImagesInOrder() {
        List<Image> desired = List.of(squareImage(16), squareImage(32));
        // Window.getIconImages() returns a new list holding the same Image references
        assertTrue(BurpTitleAndIcon.iconsAlreadyApplied(new java.util.ArrayList<>(desired), desired));
    }

    @Test
    void iconsAlreadyAppliedIsFalseForDifferentImagesOrSizes() {
        List<Image> desired = List.of(squareImage(16), squareImage(32));
        // equal looking but different Image instances mean Burp replaced the icons
        assertFalse(BurpTitleAndIcon.iconsAlreadyApplied(List.of(squareImage(16), squareImage(32)), desired));
        // a shorter, longer, or missing current list must trigger a reapply
        assertFalse(BurpTitleAndIcon.iconsAlreadyApplied(List.of(desired.get(0)), desired));
        assertFalse(BurpTitleAndIcon.iconsAlreadyApplied(List.of(), desired));
        assertFalse(BurpTitleAndIcon.iconsAlreadyApplied(null, desired));
    }

    @Test
    void iconsAlreadyAppliedIsFalseWhenTheOrderChanged() {
        List<Image> desired = List.of(squareImage(16), squareImage(32));
        assertFalse(BurpTitleAndIcon.iconsAlreadyApplied(List.of(desired.get(1), desired.get(0)), desired));
    }

    // The main frame is mocked because a real JFrame cannot be created in a headless test
    private static BurpExtensionSharedParameters mockSharedParametersWithFrame(JFrame frame) {
        BurpExtensionSharedParameters sharedParameters = mock(BurpExtensionSharedParameters.class);
        when(sharedParameters.get_mainFrameUsingMontoya()).thenReturn(frame);
        return sharedParameters;
    }

    @Test
    void installStoresTheAddedFocusListener() {
        JFrame frame = mock(JFrame.class);
        BurpExtensionSharedParameters sharedParameters = mockSharedParametersWithFrame(frame);

        BurpTitleAndIcon.installIconRefreshListener(sharedParameters, List.of(squareImage(16)));

        ArgumentCaptor<WindowFocusListener> added = ArgumentCaptor.forClass(WindowFocusListener.class);
        verify(frame).addWindowFocusListener(added.capture());
        assertSame(added.getValue(), sharedParameters.iconRefreshWindowFocusListener);
    }

    @Test
    void removeOnlyRemovesTheStoredListenerAndClearsIt() {
        // a memory leak regression test: unload used to remove the LAST focus listener on the
        // main frame, which could be one owned by Burp or another extension, and in that case
        // our own listener stayed on the frame forever
        JFrame frame = mock(JFrame.class);
        BurpExtensionSharedParameters sharedParameters = mockSharedParametersWithFrame(frame);

        BurpTitleAndIcon.installIconRefreshListener(sharedParameters, List.of(squareImage(16)));
        WindowFocusListener ourListener = sharedParameters.iconRefreshWindowFocusListener;

        BurpTitleAndIcon.removeMainFrameWindowFocusListener(sharedParameters);

        verify(frame).removeWindowFocusListener(same(ourListener));
        verify(frame, times(1)).removeWindowFocusListener(any());
        assertNull(sharedParameters.iconRefreshWindowFocusListener);
    }

    @Test
    void removeDoesNothingWhenNoListenerWasInstalled() {
        JFrame frame = mock(JFrame.class);
        BurpExtensionSharedParameters sharedParameters = mockSharedParametersWithFrame(frame);

        BurpTitleAndIcon.removeMainFrameWindowFocusListener(sharedParameters);

        verify(frame, never()).removeWindowFocusListener(any());
    }

    @Test
    void installingAgainReplacesTheOldListenerInsteadOfStacking() {
        JFrame frame = mock(JFrame.class);
        BurpExtensionSharedParameters sharedParameters = mockSharedParametersWithFrame(frame);

        BurpTitleAndIcon.installIconRefreshListener(sharedParameters, List.of(squareImage(16)));
        WindowFocusListener firstListener = sharedParameters.iconRefreshWindowFocusListener;

        BurpTitleAndIcon.installIconRefreshListener(sharedParameters, List.of(squareImage(16)));

        verify(frame).removeWindowFocusListener(same(firstListener));
        assertNotSame(firstListener, sharedParameters.iconRefreshWindowFocusListener);
    }
}
