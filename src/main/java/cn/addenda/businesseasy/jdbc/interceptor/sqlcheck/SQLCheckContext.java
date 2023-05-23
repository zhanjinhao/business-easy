package cn.addenda.businesseasy.jdbc.interceptor.sqlcheck;

/**
 * @author addenda
 * @since 2023/5/7 15:55
 */
public class SQLCheckContext {

    private SQLCheckContext() {
    }

    private static final ThreadLocal<Boolean> CHECK_ALL_COLUMN_TL = ThreadLocal.withInitial(() -> null);
    private static final ThreadLocal<Boolean> CHECK_EXACT_IDENTIFIER_TL = ThreadLocal.withInitial(() -> null);
    private static final ThreadLocal<Boolean> CHECK_DML_CONDITION_TL = ThreadLocal.withInitial(() -> null);

    public static void setCheckAllColumn(boolean check) {
        CHECK_ALL_COLUMN_TL.set(check);
    }

    public static void setCheckExactIdentifier(boolean check) {
        CHECK_EXACT_IDENTIFIER_TL.set(check);
    }

    public static void setCheckDmlCondition(boolean check) {
        CHECK_DML_CONDITION_TL.set(check);
    }

    public static Boolean getCheckAllColumn() {
        return CHECK_ALL_COLUMN_TL.get();
    }

    public static Boolean getCheckExactIdentifier() {
        return CHECK_EXACT_IDENTIFIER_TL.get();
    }

    public static Boolean getCheckDmlCondition() {
        return CHECK_DML_CONDITION_TL.get();
    }

    public static void clearCheckAllColumn() {
        CHECK_ALL_COLUMN_TL.remove();
    }

    public static void clearCheckExactIdentifier() {
        CHECK_EXACT_IDENTIFIER_TL.remove();
    }

    public static void clearCheckDmlCondition() {
        CHECK_DML_CONDITION_TL.remove();
    }

    public static void clear() {
        clearCheckAllColumn();
        clearCheckExactIdentifier();
        clearCheckDmlCondition();
    }

}
