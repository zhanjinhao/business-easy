package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

/**
 * @author addenda
 * @since 2023/5/17 22:42
 */
public class TombstoneContext {

    private TombstoneContext() {
    }

    private static final ThreadLocal<Boolean> JOIN_USE_SUB_QUERY_TL = ThreadLocal.withInitial(() -> null);

    public static void setJoinUseSubQuery(boolean useSubQuery) {
        JOIN_USE_SUB_QUERY_TL.set(useSubQuery);
    }

    public static Boolean getJoinUseSubQuery() {
        return JOIN_USE_SUB_QUERY_TL.get();
    }

    public static void clearJoinUseSubQuery() {
        JOIN_USE_SUB_QUERY_TL.remove();
    }

}
