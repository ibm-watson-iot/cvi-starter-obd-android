/**
 * Copyright 2016 IBM Corp. All Rights Reserved.
 * <p>
 * Licensed under the IBM License, a copy of which may be obtained at:
 * <p>
 * http://www14.software.ibm.com/cgi-bin/weblap/lap.pl?li_formnum=L-DDIN-AEGGZJ&popup=y&title=IBM%20IoT%20for%20Automotive%20Sample%20Starter%20Apps%20%28Android-Mobile%20and%20Server-all%29
 * <p>
 * You may not use this file except in compliance with the license.
 */

package obdii.starter.automotive.iot.ibm.com.iot4a_obdii;

public class PeriodicExecutor {

    public static interface Task {
        public void initialize();

        public boolean run();

        public void finalize();
    }

    private int delay;
    private int period;
    private Task task;
    private Thread thread = null;
    private final Object object = new Object();

    public PeriodicExecutor(final int initDelay, final int initPeriod) {
        delay = initDelay;
        period = initPeriod;
    }

    public synchronized void schedule(final Task task) {
        this.task = task;
        restart();
    }

    private synchronized void restart() {
        cancel();
        if (task == null) {
            return;
        }
        if (thread == null) {
            thread = new Thread(new java.lang.Runnable() {

                @Override
                public void run() {
                    final Thread mythread = thread;
                    try {
                        task.initialize();
                        synchronized (object) {
                            object.wait(delay);
                        }
                    } catch (InterruptedException e) {
                        mythread.interrupt();
                    }
                    try {
                        while (!mythread.isInterrupted()) {
                            try {
                                if (mythread != null && !mythread.isInterrupted()) {
                                    if (!task.run()) {
                                        mythread.interrupt();
                                        break;
                                    }
                                }
                                synchronized (object) {
                                    object.wait(period);
                                }
                            } catch (InterruptedException e) {
                                mythread.interrupt();
                            }
                        }
                    } finally {
                        task.finalize();
                    }
                }
            });
            thread.start();
        }
    }

    public synchronized void cancel() {
        if (thread != null) {
            synchronized (object) {
                thread.interrupt();
                object.notifyAll();
                thread = null;
            }
        }
    }

    public void changePeriod(final int delay, final int period) {
        this.delay = delay;
        this.period = period;
        restart();
    }
}
