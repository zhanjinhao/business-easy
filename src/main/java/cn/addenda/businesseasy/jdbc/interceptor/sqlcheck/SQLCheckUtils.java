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

    public static <T> T unCheck(boolean checkAllColumn, boolean checkExactIdentifier, SQLCheckExecutor<T> executor) {
        try {
            SQLCheckContext.setCheckAllColumn(checkAllColumn);
            SQLCheckContext.setCheckExactIdentifier(checkExactIdentifier);
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

    public static void unCheck(boolean checkAllColumn, boolean checkExactIdentifier, VoidSQLCheckExecutor executor) {
        try {
            SQLCheckContext.setCheckAllColumn(checkAllColumn);
            SQLCheckContext.setCheckExactIdentifier(checkExactIdentifier);
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
