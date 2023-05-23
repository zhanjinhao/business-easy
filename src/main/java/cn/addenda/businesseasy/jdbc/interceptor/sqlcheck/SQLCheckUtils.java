package cn.addenda.businesseasy.jdbc.interceptor.sqlcheck;

import cn.addenda.businesseasy.util.ExceptionUtil;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author addenda
 * @since 2023/5/7 15:55
 */
public class SQLCheckUtils {

    private SQLCheckUtils() {
    }

    public static <T> T unCheckAllColumn(SQLCheckExecutor<T> executor) {
        try {
            SQLCheckContext.setCheckAllColumn(false);
            return executor.execute();
        } catch (Throwable e) {
            reportAsRuntimeException(e);
            // todo: SystemException 迁移过来
            return null;
        } finally {
            SQLCheckContext.clearCheckAllColumn();
        }
    }

    public static <T> T unCheckExactIdentifier(SQLCheckExecutor<T> executor) {
        try {
            SQLCheckContext.setCheckExactIdentifier(false);
            return executor.execute();
        } catch (Throwable e) {
            reportAsRuntimeException(e);
            // todo: SystemException 迁移过来
            return null;
        } finally {
            SQLCheckContext.clearCheckExactIdentifier();
        }
    }

    public static <T> T unCheckDmlCondition(SQLCheckExecutor<T> executor) {
        try {
            SQLCheckContext.setCheckDmlCondition(false);
            return executor.execute();
        } catch (Throwable e) {
            reportAsRuntimeException(e);
            // todo: SystemException 迁移过来
            return null;
        } finally {
            SQLCheckContext.clearCheckDmlCondition();
        }
    }

    public static <T> T config(SQLCheckExecutor<T> executor, boolean... unChecks) {
        try {
            if (unChecks != null) {
                if (unChecks.length > 0) {
                    SQLCheckContext.setCheckAllColumn(unChecks[0]);
                }
                if (unChecks.length > 1) {
                    SQLCheckContext.setCheckExactIdentifier(unChecks[1]);
                }
                if (unChecks.length > 2) {
                    SQLCheckContext.setCheckDmlCondition(unChecks[2]);
                }
            }
            return executor.execute();
        } catch (Throwable e) {
            reportAsRuntimeException(e);
            // todo: SystemException 迁移过来
            return null;
        } finally {
            SQLCheckContext.clear();
        }
    }

    public static void unCheckAllColumn(VoidSQLCheckExecutor executor) {
        try {
            SQLCheckContext.setCheckAllColumn(false);
            executor.execute();
        } catch (Throwable e) {
            reportAsRuntimeException(e);
        } finally {
            SQLCheckContext.clearCheckAllColumn();
        }
    }

    public static void unCheckExactIdentifier(VoidSQLCheckExecutor executor) {
        try {
            SQLCheckContext.setCheckExactIdentifier(false);
            executor.execute();
        } catch (Throwable e) {
            reportAsRuntimeException(e);
        } finally {
            SQLCheckContext.clearCheckExactIdentifier();
        }
    }

    public static void unCheckDmlCondition(VoidSQLCheckExecutor executor) {
        try {
            SQLCheckContext.setCheckDmlCondition(false);
            executor.execute();
        } catch (Throwable e) {
            reportAsRuntimeException(e);
        } finally {
            SQLCheckContext.clearCheckDmlCondition();
        }
    }

    public static void config(VoidSQLCheckExecutor executor, boolean... unChecks) {
        try {
            if (unChecks != null) {
                if (unChecks.length > 0) {
                    SQLCheckContext.setCheckAllColumn(unChecks[0]);
                }
                if (unChecks.length > 1) {
                    SQLCheckContext.setCheckExactIdentifier(unChecks[1]);
                }
                if (unChecks.length > 2) {
                    SQLCheckContext.setCheckDmlCondition(unChecks[2]);
                }
            }
            executor.execute();
        } catch (Throwable e) {
            reportAsRuntimeException(e);
        } finally {
            SQLCheckContext.clear();
        }
    }

    protected static void reportAsRuntimeException(Throwable throwable) {
        throwable = ExceptionUtil.unwrapThrowable(throwable);
        if (!(throwable instanceof SQLCheckException)) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else {
                throw new UndeclaredThrowableException(throwable);
            }
        }

        throw (SQLCheckException) throwable;
    }

    public interface SQLCheckExecutor<T> {

        T execute() throws Throwable;
    }

    public interface VoidSQLCheckExecutor {

        void execute() throws Throwable;
    }

}
