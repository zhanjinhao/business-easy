package cn.addenda.businesseasy.lock;

public interface LockService {

    boolean tryLock(String lockName);

    boolean tryLock(String lockName, Long threadId);

    void unlock(String lockName);

    void unlock(String lockName, Long threadId);

    void forceUnlock(String lockName);

}
