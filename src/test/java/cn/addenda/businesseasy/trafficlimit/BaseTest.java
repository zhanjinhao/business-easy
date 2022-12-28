package cn.addenda.businesseasy.trafficlimit;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author addenda
 * @datetime 2022/12/28 18:59
 */
public class BaseTest {

    SecureRandom r = new SecureRandom();

    TrafficLimiter trafficLimiter;

    public BaseTest(TrafficLimiter trafficLimiter) {
        this.trafficLimiter = trafficLimiter;
    }

    public void test() throws Exception {
        List<Thread> threadList = new ArrayList<>();
        BlockingQueue<Long> blockingQueue = new LinkedBlockingDeque<>();
        for (int i = 0; i < 100; i++) {
            threadList.add(new Thread(() -> {
                while (true) {
                    boolean b = trafficLimiter.tryAcquire();
                    if (b) {
                        blockingQueue.offer(System.currentTimeMillis());
                    }
                    try {
                        Thread.sleep(r.nextInt(50) + 50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
        }

        new Thread(() -> {
            while (true) {
                try {
                    Long take = blockingQueue.take();
                    System.out.println(take);
                } catch (InterruptedException e) {

                }
            }
        }).start();

        for (Thread thread : threadList) {
            thread.start();
        }

        while (true) {
            Thread.sleep(10000);
        }
    }
}
