package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

/**
 * @author addenda
 * @since 2023/5/3 18:13
 */
public class BaseEntityContext {

    private BaseEntityContext() {
    }

    private static final ThreadLocal<String> MASTER_VIEW_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);

    public static void setMasterView(String masterView) {
        MASTER_VIEW_THREAD_LOCAL.set(masterView);
    }

    public static void clearMasterView() {
        MASTER_VIEW_THREAD_LOCAL.remove();
    }

    public static String getMasterView() {
        return MASTER_VIEW_THREAD_LOCAL.get();
    }

}
