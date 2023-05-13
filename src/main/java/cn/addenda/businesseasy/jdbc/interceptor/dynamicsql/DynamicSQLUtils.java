package cn.addenda.businesseasy.jdbc.interceptor.dynamicsql;

import java.util.function.Supplier;

/**
 * @author addenda
 * @since 2023/5/13 12:23
 */
public class DynamicSQLUtils {

    private DynamicSQLUtils() {
    }

    public static void tableAddJoinCondition(String tableName, String condition, Runnable runnable) {
        DynamicSQLContext.tableAddJoinCondition(tableName, condition);
        try {
            runnable.run();
        } finally {
            DynamicSQLContext.clearCondition();
        }
    }

    public static void tableAddJoinCondition(String condition, Runnable runnable) {
        tableAddJoinCondition(DynamicSQLContext.ALL_TABLE, condition, runnable);
    }

    public static <T> T tableAddJoinCondition(String tableName, String condition, Supplier<T> supplier) {
        DynamicSQLContext.tableAddJoinCondition(tableName, condition);
        try {
            return supplier.get();
        } finally {
            DynamicSQLContext.clearCondition();
        }
    }

    public static <T> T tableAddJoinCondition(String condition, Supplier<T> supplier) {
        return tableAddJoinCondition(DynamicSQLContext.ALL_TABLE, condition, supplier);
    }

    public static void viewAddJoinCondition(String tableName, String condition, Runnable runnable) {
        DynamicSQLContext.viewAddJoinCondition(tableName, condition);
        try {
            runnable.run();
        } finally {
            DynamicSQLContext.clearCondition();
        }
    }

    public static void viewAddJoinCondition(String condition, Runnable runnable) {
        viewAddJoinCondition(DynamicSQLContext.ALL_TABLE, condition, runnable);
    }

    public static <T> T viewAddJoinCondition(String tableName, String condition, Supplier<T> supplier) {
        DynamicSQLContext.viewAddJoinCondition(tableName, condition);
        try {
            return supplier.get();
        } finally {
            DynamicSQLContext.clearCondition();
        }
    }

    public static <T> T viewAddJoinCondition(String condition, Supplier<T> supplier) {
        return viewAddJoinCondition(DynamicSQLContext.ALL_TABLE, condition, supplier);
    }

    public static void tableAddWhereCondition(String tableName, String condition, Runnable runnable) {
        DynamicSQLContext.tableAddWhereCondition(tableName, condition);
        try {
            runnable.run();
        } finally {
            DynamicSQLContext.clearCondition();
        }
    }

    public static void tableAddWhereCondition(String condition, Runnable runnable) {
        tableAddWhereCondition(DynamicSQLContext.ALL_TABLE, condition, runnable);
    }

    public static <T> T tableAddWhereCondition(String tableName, String condition, Supplier<T> supplier) {
        DynamicSQLContext.tableAddWhereCondition(tableName, condition);
        try {
            return supplier.get();
        } finally {
            DynamicSQLContext.clearCondition();
        }
    }

    public static <T> T tableAddWhereCondition(String condition, Supplier<T> supplier) {
        return tableAddWhereCondition(DynamicSQLContext.ALL_TABLE, condition, supplier);
    }

    public static void viewAddWhereCondition(String tableName, String condition, Runnable runnable) {
        DynamicSQLContext.tableAddWhereCondition(tableName, condition);
        try {
            runnable.run();
        } finally {
            DynamicSQLContext.clearCondition();
        }
    }

    public static void viewAddWhereCondition(String condition, Runnable runnable) {
        viewAddWhereCondition(DynamicSQLContext.ALL_TABLE, condition, runnable);
    }

    public static <T> T viewAddWhereCondition(String tableName, String condition, Supplier<T> supplier) {
        DynamicSQLContext.tableAddWhereCondition(tableName, condition);
        try {
            return supplier.get();
        } finally {
            DynamicSQLContext.clearCondition();
        }
    }

    public static <T> T viewAddWhereCondition(String condition, Supplier<T> supplier) {
        return viewAddWhereCondition(DynamicSQLContext.ALL_TABLE, condition, supplier);
    }

    public static void insertAddItem(String tableName, String itemKey, Object itemValue, Runnable runnable) {
        DynamicSQLContext.insertAddItem(tableName, itemKey, itemValue);
        try {
            runnable.run();
        } finally {
            DynamicSQLContext.clearItem();
        }
    }

    public static void insertAddItem(String itemKey, Object itemValue, Runnable runnable) {
        DynamicSQLContext.insertAddItem(DynamicSQLContext.ALL_TABLE, itemKey, itemValue);
        try {
            runnable.run();
        } finally {
            DynamicSQLContext.clearItem();
        }
    }

    public static <T> T insertAddItem(String tableName, String itemKey, Object itemValue, Supplier<T> supplier) {
        DynamicSQLContext.insertAddItem(tableName, itemKey, itemValue);
        try {
            return supplier.get();
        } finally {
            DynamicSQLContext.clearItem();
        }
    }

    public static <T> T insertAddItem(String itemKey, Object itemValue, Supplier<T> supplier) {
        return insertAddItem(DynamicSQLContext.ALL_TABLE, itemKey, itemValue, supplier);
    }

    public static void updateAddItem(String tableName, String itemKey, Object itemValue, Runnable runnable) {
        DynamicSQLContext.updateAddItem(tableName, itemKey, itemValue);
        try {
            runnable.run();
        } finally {
            DynamicSQLContext.clearItem();
        }
    }

    public static void updateAddItem(String itemKey, Object itemValue, Runnable runnable) {
        updateAddItem(DynamicSQLContext.ALL_TABLE, itemKey, itemValue, runnable);
    }

    public static <T> T updateAddItem(String tableName, String itemKey, Object itemValue, Supplier<T> supplier) {
        DynamicSQLContext.updateAddItem(tableName, itemKey, itemValue);
        try {
            return supplier.get();
        } finally {
            DynamicSQLContext.clearItem();
        }
    }

    public static <T> T updateAddItem(String itemKey, Object itemValue, Supplier<T> supplier) {
        return updateAddItem(DynamicSQLContext.ALL_TABLE, itemKey, itemValue, supplier);
    }

}
