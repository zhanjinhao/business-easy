package cn.addenda.businesseasy.util;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author addenda
 * @datetime 2023/1/10 22:26
 */
@Slf4j
public class BeWaitUtils {

    private BeWaitUtils() {
    }

    /**
     * 方法的执行必须在monitor的监视范围内
     */
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

    /**
     * 方法的执行必须在monitor的监视范围内
     */
    public static void waitWithoutLockInterruptibly(TimeUnit timeUnit, long timeout, Object monitor) {
        long timeoutMillis = timeUnit.toMillis(timeout);
        try {
            monitor.wait(timeoutMillis);
        } catch (InterruptedException e) {
        }
    }

}
