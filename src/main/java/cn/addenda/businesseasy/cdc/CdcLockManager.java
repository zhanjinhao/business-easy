package cn.addenda.businesseasy.cdc;

/**
 * @Author ISJINHAO
 * @Date 2022/4/16 15:13
 */
public interface CdcLockManager {

    boolean tryLock(String lockName);

    void releaseLock(String lockName);

}
