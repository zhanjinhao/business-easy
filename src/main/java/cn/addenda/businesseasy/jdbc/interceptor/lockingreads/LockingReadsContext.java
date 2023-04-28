package cn.addenda.businesseasy.jdbc.interceptor.lockingreads;

/**
 * @author addenda
 * @datetime 2023/4/27 20:21
 */
public class LockingReadsContext {

    private LockingReadsContext() {
    }

    public static final String W_LOCK = "W";
    public static final String R_LOCK = "R";

    private static final ThreadLocal<String> LOCK_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);

    public static void setLock(String lock) {
        if (!W_LOCK.equals(lock) && !R_LOCK.equals(lock)) {
            throw new LockingReadsException("不支持的LOCK类型，当前LOCK类型：" + lock + "。");
        }
        LOCK_THREAD_LOCAL.set(lock);
    }

    public static void clearLock() {
        LOCK_THREAD_LOCAL.remove();
    }

    public static String getLock() {
        return LOCK_THREAD_LOCAL.get();
    }

}
