package cn.addenda.businesseasy.jdbc.interceptor.lockingreads;

/**
 * @author addenda
 * @datetime 2023/4/27 20:25
 */
public class LockingReadsUtils {

    private LockingReadsUtils() {
    }

    public static <T> T rSelect(LockingReadsExecutor<T> executor) {
        try {
            LockingReadsContext.setLock(LockingReadsContext.R_LOCK);
            return executor.execute();
        } catch (Throwable throwable) {
            throw new LockingReadsException(throwable);
        } finally {
            LockingReadsContext.clearLock();
        }
    }

    public static <T> T wSelect(LockingReadsExecutor<T> executor) {
        try {
            LockingReadsContext.setLock(LockingReadsContext.W_LOCK);
            return executor.execute();
        } catch (Throwable throwable) {
            throw new LockingReadsException(throwable);
        } finally {
            LockingReadsContext.clearLock();
        }
    }

    public static <T> T select(String lock, LockingReadsExecutor<T> executor) {
        if (LockingReadsContext.W_LOCK.equals(lock)) {
            return wSelect(executor);
        } else if (LockingReadsContext.R_LOCK.equals(lock)) {
            return rSelect(executor);
        } else {
            throw new LockingReadsException("不支持的LOCK类型，当前LOCK类型：" + lock + "。");
        }
    }

    public static void rSelect(VoidLockingReadsExecutor executor) {
        try {
            LockingReadsContext.setLock(LockingReadsContext.R_LOCK);
            executor.execute();
        } catch (Throwable throwable) {
            throw new LockingReadsException(throwable);
        } finally {
            LockingReadsContext.clearLock();
        }
    }

    public static void wSelect(VoidLockingReadsExecutor executor) {
        try {
            LockingReadsContext.setLock(LockingReadsContext.W_LOCK);
            executor.execute();
        } catch (Throwable throwable) {
            throw new LockingReadsException(throwable);
        } finally {
            LockingReadsContext.clearLock();
        }
    }

    public static void select(String lock, VoidLockingReadsExecutor executor) {
        if (LockingReadsContext.W_LOCK.equals(lock)) {
            wSelect(executor);
        } else if (LockingReadsContext.R_LOCK.equals(lock)) {
            rSelect(executor);
        } else {
            throw new LockingReadsException("不支持的LOCK类型，当前LOCK类型：" + lock + "。");
        }
    }

    public interface LockingReadsExecutor<T> {

        T execute() throws Throwable;
    }

    public interface VoidLockingReadsExecutor {

        void execute() throws Throwable;
    }

}
