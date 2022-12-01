package cn.addenda.businesseasy.lock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author addenda
 * @datetime 2022/11/30 20:29
 */
public class LockDiffThreadTest {

    private static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws Exception {

        new Thread(() -> {
            System.out.println("线程1加锁 开始！");
            lock.lock();
            System.out.println("线程1加锁 成功！");

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }).start();

        Thread.sleep(1000L);

        new Thread(() -> {
            System.out.println("线程2解锁 开始！");
            lock.unlock();
            System.out.println("线程2解锁 成功！");
        }).start();

    }

}
