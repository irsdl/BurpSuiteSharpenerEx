// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

// The clipboard read used to run synchronously on the EDT while the tab context
// menu was being built. When another process owned the clipboard and was slow to
// answer, the whole Burp UI froze. The read now runs on a background thread and
// only the result is delivered on the EDT.
public class SubTabsActionsClipboardTest {

    @Test
    void normalizeStripsTheNumberPrefixAndTrims() {
        assertEquals("my title", SubTabsActions.normalizeClipboardTitle("  #12 my title  "));
        assertEquals("my title", SubTabsActions.normalizeClipboardTitle("#7 my title"));
    }

    @Test
    void normalizeKeepsTitlesWithoutASeparatedNumberPrefix() {
        assertEquals("plain title", SubTabsActions.normalizeClipboardTitle("plain title"));
        assertEquals("#7title", SubTabsActions.normalizeClipboardTitle("#7title"));
        assertEquals("# 7 title", SubTabsActions.normalizeClipboardTitle("# 7 title"));
    }

    @Test
    void normalizeTurnsNullAndBlankIntoAnEmptyString() {
        assertEquals("", SubTabsActions.normalizeClipboardTitle(null));
        assertEquals("", SubTabsActions.normalizeClipboardTitle("   "));
    }

    @Test
    void asyncReadDeliversTheResultOnTheEdtAndNeverBlocksTheCaller() throws Exception {
        ExtensionSharedParameters sharedParameters = mock(ExtensionSharedParameters.class);
        CountDownLatch delivered = new CountDownLatch(1);
        AtomicBoolean onEdt = new AtomicBoolean(false);
        AtomicReference<String> received = new AtomicReference<>();

        SubTabsActions.readClipboardTitleAsync(sharedParameters, value -> {
            onEdt.set(javax.swing.SwingUtilities.isEventDispatchThread());
            received.set(value);
            delivered.countDown();
        });

        assertTrue(delivered.await(10, TimeUnit.SECONDS), "the clipboard result was never delivered");
        assertTrue(onEdt.get(), "the result must be delivered on the EDT");
        // the value depends on the environment (headless reads yield an empty string),
        // but it is never null and always matches the stored last clipboard text
        assertNotNull(received.get());
        assertEquals(sharedParameters.lastClipboardText, received.get());
    }
}
