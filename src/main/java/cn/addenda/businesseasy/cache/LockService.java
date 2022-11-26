package cn.addenda.businesseasy.cache;

public interface LockService {

    boolean tryLock(String lockName);

    void unlock(String lockName);

    void forceUnlock(String lockName);

}
