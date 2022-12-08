package cn.addenda.businesseasy.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RedissonLockService implements DistributeLockService {

    private final Map<String, RLock> redissonLockMap = new ConcurrentHashMap<>();

    private final RedissonClient redissonClient;

    public RedissonLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String lockName) {
        return redissonLockMap.computeIfAbsent(lockName, s -> redissonClient.getLock(lockName)).tryLock();
    }

    @Override
    public boolean tryLock(String lockName, Long threadId) {
        RFuture<Boolean> future = redissonLockMap.computeIfAbsent(
                lockName, s -> redissonClient.getLock(lockName)).tryLockAsync(threadId);
        try {
            future.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (future.isSuccess()) {
            return future.getNow();
        }

        Throwable e;
        if (future.cause() instanceof RedisException) {
            e = future.cause();
        } else {
            e = new RedisException("Unexpected exception while processing command", future.cause());
        }

        log.error("Unexpected exception while tryLock(), lockName: [{}], threadId: [{}] .", lockName, threadId, e);
        throw new LockException("Unexpected exception while tryLock(), lockName: [" + lockName + "], threadId: [" + threadId + "] .", e);
    }

    @Override
    public void unlock(String lockName) {
        redissonLockMap.computeIfAbsent(lockName, s -> redissonClient.getLock(lockName)).unlock();
    }

    @Override
    public void unlock(String lockName, Long threadId) {
        RFuture<Void> future = redissonLockMap.computeIfAbsent(lockName, s -> redissonClient.getLock(lockName)).unlockAsync(threadId);
        try {
            future.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (future.isSuccess()) {
            future.getNow();
            return;
        }

        Throwable e;
        if (future.cause() instanceof RedisException) {
            e = future.cause();
        } else {
            e = new RedisException("Unexpected exception while processing command", future.cause());
        }

        if (e.getCause() instanceof IllegalMonitorStateException) {
            e = e.getCause();
        }

        log.error("Unexpected exception while unlock(), lockName(): [{}], threadId: [{}] .", lockName, threadId, e);
        throw new LockException("Unexpected exception while unlock(), lockName: [" + lockName + "], threadId: [" + threadId + "] .", e);
    }

    @Override
    public void forceUnlock(String lockName) {
        redissonLockMap.computeIfAbsent(lockName, s -> redissonClient.getLock(lockName)).forceUnlock();
    }

}
