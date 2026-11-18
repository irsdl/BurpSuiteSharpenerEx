// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class BackgroundTaskRunnerTest {
    @Test
    void failuresAreReportedAndDoNotPreventTheNextTask() throws Exception {
        AtomicReference<Throwable> reported = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        BackgroundTaskRunner runner = new BackgroundTaskRunner((feature, failure) -> reported.set(failure));
        try {
            assertTrue(runner.submit("changed API", () -> { throw new NoSuchMethodError("missing"); }));
            assertTrue(runner.submit("next", done::countDown));
            assertTrue(done.await(3, TimeUnit.SECONDS));
            assertInstanceOf(NoSuchMethodError.class, reported.get());
        } finally { runner.stop(); }
    }

    @Test
    void slowWorkRunsOffTheEdt() throws Exception {
        AtomicBoolean onEdt = new AtomicBoolean(true);
        CountDownLatch done = new CountDownLatch(1);
        BackgroundTaskRunner runner = new BackgroundTaskRunner((feature, failure) -> fail(failure));
        try {
            SwingUtilities.invokeAndWait(() -> runner.submit("test", () -> {
                onEdt.set(SwingUtilities.isEventDispatchThread());
                done.countDown();
            }));
            assertTrue(done.await(3, TimeUnit.SECONDS));
            assertFalse(onEdt.get());
        } finally { runner.stop(); }
    }

    @Test
    void stopInterruptsActiveWorkDropsQueuedWorkAndRejectsNewWork() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicBoolean queuedRan = new AtomicBoolean();
        BackgroundTaskRunner runner = new BackgroundTaskRunner((feature, failure) -> fail(failure));
        try {
            runner.submit("active", () -> {
                started.countDown();
                try { new CountDownLatch(1).await(); }
                finally { interrupted.countDown(); }
            });
            assertTrue(started.await(3, TimeUnit.SECONDS));
            runner.submit("queued", () -> queuedRan.set(true));
            SwingUtilities.invokeAndWait(runner::stop);
            assertTrue(interrupted.await(3, TimeUnit.SECONDS));
            assertTrue(runner.isStopped());
            assertFalse(queuedRan.get());
            assertFalse(runner.submit("late", () -> queuedRan.set(true)));
            assertDoesNotThrow(runner::stop);
        } finally { runner.stop(); }
    }
}
