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
        return hasPrefix(sql, "select");
    }

    public static boolean isUpdate(String sql) {
        return hasPrefix(sql, "update");
    }

    public static boolean isDelete(String sql) {
        return hasPrefix(sql, "delete");
    }

    public static boolean isInsert(String sql) {
        return hasPrefix(sql, "isnert");
    }

    public static boolean hasPrefix(String sql, String base) {
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

    public static boolean include(
            String tableName, List<String> included, List<String> notIncluded) {
        if (notIncluded != null) {
            for (String unContain : notIncluded) {
                if (unContain.equalsIgnoreCase(tableName)) {
                    return false;
                }
            }
        }
        if (included == null) {
            return true;
        }
        for (String contain : included) {
            if (contain.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }

    public static String extractColumnName(String value) {
        int i = value.indexOf(".");
        if (i == -1) {
            return value;
        }
        return value.substring(i + 1);
    }

    public static String extractColumnOwner(String value) {
        int i = value.indexOf(".");
        if (i == -1) {
            return null;
        }
        return value.substring(0, i);
    }

    public static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }

    public static <T> T getOrDefault(T get, T _default) {
        if (get == null) {
            return _default;
        }
        return get;
    }

}
