package cn.addenda.businesseasy.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author addenda
 * @datetime 2023/1/10 22:26
 */
@Slf4j
public class CallerWaitUtils {

    public static void waitWithLock(TimeUnit timeUnit, long timeout) {
        long start = System.currentTimeMillis();
        long timeoutMillis = timeUnit.toMillis(timeout);
        long now = 0;
        while (true) {
            long delay = timeoutMillis - now;
            if (delay <= 0) {
                break;
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // ignore ...
            }
            now = System.currentTimeMillis() - start;
        }
    }

    public static void waitWithLockInterruptibly(TimeUnit timeUnit, long timeout) {
        try {
            timeUnit.sleep(timeout);
        } catch (InterruptedException e) {
        }
    }

    public static void waitWithoutLock(TimeUnit timeUnit, long timeout, Object monitor) {
        long start = System.currentTimeMillis();
        long timeoutMillis = timeUnit.toMillis(timeout);
        long now = 0;
        while (true) {
            long delay = timeoutMillis - now;
            if (delay <= 0) {
                break;
            }
            try {
                monitor.wait(delay);
            } catch (InterruptedException e) {
                // ignore ...
            }
            now = System.currentTimeMillis() - start;
        }
    }

    public static void waitWithoutLockInterruptibly(TimeUnit timeUnit, long timeout, Object monitor) {
        long timeoutMillis = timeUnit.toMillis(timeout);
        try {
            monitor.wait(timeoutMillis);
        } catch (InterruptedException e) {
        }
    }

}
