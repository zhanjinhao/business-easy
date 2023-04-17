package cn.addenda.businesseasy.util;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author addenda
 * @datetime 2023/3/9 19:45
 */
@Slf4j
public class BESleepUtilsTest {

    @Test
    public void main() {
        Thread thread = new Thread(() -> {
            log.info("start. ");
            BESleepUtils.sleep(TimeUnit.SECONDS, 30, false);
            log.info("end. ");
            if (Thread.currentThread().isInterrupted()) {
                log.info("睡眠期间被打断了！");
            }
        });
        thread.start();

        while (thread.isAlive()) {
            BESleepUtils.sleep(TimeUnit.SECONDS, 3, false);
            thread.interrupt();
        }
    }

}
