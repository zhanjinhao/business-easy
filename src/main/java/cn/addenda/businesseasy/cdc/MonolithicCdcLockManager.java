package cn.addenda.businesseasy.cdc;

/**
 * 单体应用的 LockManager
 *
 * @Author ISJINHAO
 * @Date 2022/4/16 15:17
 */
public class MonolithicCdcLockManager implements CdcLockManager {

    @Override
    public boolean tryLock(String lockName) {
        return true;
    }

    @Override
    public void releaseLock(String lockName) {
        // no-op
    }

}
