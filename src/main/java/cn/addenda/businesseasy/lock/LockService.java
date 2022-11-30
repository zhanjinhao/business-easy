package cn.addenda.businesseasy.lock;

public interface LockService {

    boolean tryLock(String lockName);

    void unlock(String lockName);

    void forceUnlock(String lockName);

}
