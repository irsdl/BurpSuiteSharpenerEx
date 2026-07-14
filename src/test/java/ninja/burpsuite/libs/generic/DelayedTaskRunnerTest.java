// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.libs.generic;

import org.junit.jupiter.api.Test;

import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DelayedTaskRunnerTest {

    @Test
    void aScheduledTaskRunsBeforeStop() throws InterruptedException {
        DelayedTaskRunner runner = new DelayedTaskRunner();
        CountDownLatch latch = new CountDownLatch(1);

        runner.schedule(new TimerTask() {
            @Override
            public void run() {
                latch.countDown();
            }
        }, 10);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "the task should have run");
        runner.stop();
    }

    @Test
    void startsNotStopped() {
        DelayedTaskRunner runner = new DelayedTaskRunner();
        assertFalse(runner.isStopped());
    }

    @Test
    void stopSetsTheStoppedFlag() {
        DelayedTaskRunner runner = new DelayedTaskRunner();
        runner.stop();
        assertTrue(runner.isStopped());
    }

    @Test
    void schedulingAfterStopDoesNothing() throws InterruptedException {
        DelayedTaskRunner runner = new DelayedTaskRunner();
        runner.stop();

        AtomicInteger runCount = new AtomicInteger(0);
        runner.schedule(new TimerTask() {
            @Override
            public void run() {
                runCount.incrementAndGet();
            }
        }, 10);

        // wait past the delay to be sure the task is never executed
        Thread.sleep(200);
        assertEquals(0, runCount.get(), "a task scheduled after stop must not run");
    }

    @Test
    void stopIsIdempotent() {
        DelayedTaskRunner runner = new DelayedTaskRunner();
        runner.stop();
        // a second stop must not throw
        assertDoesNotThrow(runner::stop);
        assertTrue(runner.isStopped());
    }
}
