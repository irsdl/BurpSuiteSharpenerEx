// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.generic;

import java.util.Timer;
import java.util.TimerTask;

// Runs delayed tasks on one shared daemon timer.
// The extension used to create a new java.util.Timer for every delayed action.
// Those timers were never stopped, so their tasks kept firing after the
// extension was unloaded and ran against a dead Burp API. This class keeps a
// single timer that is stopped once during unload. After stop() no new task is
// scheduled and every task can check isStopped() to return early.
public class DelayedTaskRunner {
    private final Timer timer = new Timer("SharpenerSharedTimer", true);
    private volatile boolean stopped = false;

    // True after stop() has been called. Delayed tasks read this to skip work.
    public boolean isStopped() {
        return stopped;
    }

    // Schedules a task after the given delay in milliseconds.
    // Does nothing when the runner has been stopped.
    public void schedule(TimerTask task, long delay) {
        if (stopped)
            return;
        try {
            timer.schedule(task, delay);
        } catch (Exception e) {
            // the timer may have been stopped between the check above and here
        }
    }

    // Stops the timer and cancels all pending tasks. Safe to call more than once.
    public void stop() {
        stopped = true;
        try {
            timer.cancel();
        } catch (Exception e) {
            // ignore, the timer is being thrown away anyway
        }
    }
}
