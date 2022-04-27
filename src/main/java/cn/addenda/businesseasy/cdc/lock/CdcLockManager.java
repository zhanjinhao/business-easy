package cn.addenda.businesseasy.cdc.lock;

/**
 * @Author ISJINHAO
 * @Date 2022/4/16 15:13
 */
public interface CdcLockManager {

    boolean tryLock(String lockName);

    void releaseLock(String lockName);

}
