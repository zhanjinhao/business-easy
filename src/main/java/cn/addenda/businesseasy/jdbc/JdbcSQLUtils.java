package cn.addenda.businesseasy.jdbc;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/3 17:04
 */
public class JdbcSQLUtils {

    private JdbcSQLUtils() {
    }

    public static boolean isSelect(String sql) {
        return isCurd(sql, "select");
    }

    public static boolean isUpdate(String sql) {
        return isCurd(sql, "update");
    }

    public static boolean isDelete(String sql) {
        return isCurd(sql, "delete");
    }

    public static boolean isInsert(String sql) {
        return isCurd(sql, "isnert");
    }

    public static boolean isCurd(String sql, String base) {
        int length = sql.length();
        int st = 0;
        while ((st < length) && (sql.charAt(st) <= ' ')) {
            st++;
        }

        int baseLength = base.length();

        if (length - st < baseLength) {
            return false;
        }

        for (int i = 0; i < baseLength; i++) {
            if (Character.toLowerCase(base.charAt(i)) != Character.toLowerCase(sql.charAt(st + i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean contains(
            String tableName, List<String> contains, List<String> unContains) {
        if (unContains != null) {
            for (String unContain : unContains) {
                if (unContain.equalsIgnoreCase(tableName)) {
                    return false;
                }
            }
        }
        if (contains == null) {
            return true;
        }
        for (String contain : contains) {
            if (contain.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }

}
