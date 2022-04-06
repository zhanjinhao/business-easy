
package cn.addenda.businesseasy.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InternalThreadFactory.
 */
public class SimpleNamedThreadFactory implements ThreadFactory {

    private final String mPrefix;

    public SimpleNamedThreadFactory(String mPrefix) {
        this.mPrefix = mPrefix;
    }

    private final AtomicInteger mThreadNum = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable runnable) {
        String name = mPrefix + "-" + mThreadNum.getAndIncrement();
        return new Thread(runnable, name);
    }

}
