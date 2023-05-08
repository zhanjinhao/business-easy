package cn.addenda.businesseasy.jdbc.interceptor.sqlcheck;

/**
 * @author addenda
 * @since 2023/5/7 15:55
 */
public class SQLCheckContext {

    private SQLCheckContext() {
    }

    private static final ThreadLocal<Boolean> CHECK_ALL_COLUMN_THREAD_LOCAL = ThreadLocal.withInitial(() -> true);
    private static final ThreadLocal<Boolean> CHECK_EXACT_IDENTIFIER_THREAD_LOCAL = ThreadLocal.withInitial(() -> true);

    public static void setCheckAllColumn(boolean check) {
        CHECK_ALL_COLUMN_THREAD_LOCAL.set(check);
    }

    public static void setCheckExactIdentifier(boolean check) {
        CHECK_EXACT_IDENTIFIER_THREAD_LOCAL.set(check);
    }

    public static boolean getCheckAllColumn() {
        return CHECK_ALL_COLUMN_THREAD_LOCAL.get();
    }

    public static boolean getCheckExactIdentifier() {
        return CHECK_EXACT_IDENTIFIER_THREAD_LOCAL.get();
    }

    public static void clearCheckAllColumn() {
        CHECK_ALL_COLUMN_THREAD_LOCAL.remove();
    }

    public static void clearCheckExactIdentifier() {
        CHECK_EXACT_IDENTIFIER_THREAD_LOCAL.remove();
    }

    public static void clear() {
        clearCheckAllColumn();
        clearCheckExactIdentifier();
    }

}
