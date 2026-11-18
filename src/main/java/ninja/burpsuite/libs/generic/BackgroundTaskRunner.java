// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;

/** Extension-owned, bounded work queue. Shutdown never waits on Swing's EDT. */
public final class BackgroundTaskRunner {
    @FunctionalInterface
    public interface Task { void run() throws Exception; }

    private final BiConsumer<String, Throwable> report;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 2, 15,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(16),
            Thread.ofPlatform().daemon().name("SharpenerWorker-", 0).factory());

    public BackgroundTaskRunner(BiConsumer<String, Throwable> report) {
        this.report = report;
    }

    public boolean submit(String feature, Task task) {
        if (isStopped()) return false;
        try {
            executor.execute(() -> {
                if (isStopped()) return;
                try {
                    task.run();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (Exception | LinkageError failure) {
                    if (!isStopped()) report.accept(feature, failure);
                }
            });
            return true;
        } catch (RejectedExecutionException fullOrStopped) {
            if (!isStopped()) report.accept(feature, fullOrStopped);
            return false;
        }
    }

    public boolean isStopped() { return executor.isShutdown(); }

    public void stop() { executor.shutdownNow(); }
}
