package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author addenda
 * @since 2022/11/26 20:56
 */
public class DynamicConditionContext {

    private DynamicConditionContext() {
    }

    private static final ThreadLocal<Map<String, String>> TABLE_CONDITION_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);

    private static final ThreadLocal<Map<String, String>> VIEW_CONDITION_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);

    public static void addTableCondition(String tableName, String condition) {
        Map<String, String> tableConstraintMap = TABLE_CONDITION_THREAD_LOCAL.get();
        if (tableConstraintMap == null) {
            tableConstraintMap = new LinkedHashMap<>();
            TABLE_CONDITION_THREAD_LOCAL.set(tableConstraintMap);
        }
        if (tableConstraintMap.containsKey(tableName)) {
            throw new DynamicConditionException("table already exists! ");
        }
        tableConstraintMap.put(tableName, condition);
    }

    public static void addViewCondition(String tableName, String condition) {
        Map<String, String> viewConstraintMap = VIEW_CONDITION_THREAD_LOCAL.get();
        if (viewConstraintMap == null) {
            viewConstraintMap = new LinkedHashMap<>();
            VIEW_CONDITION_THREAD_LOCAL.set(viewConstraintMap);
        }
        if (viewConstraintMap.containsKey(tableName)) {
            throw new DynamicConditionException("view already exists! ");
        }
        viewConstraintMap.put(tableName, condition);
    }

    public static Map<String, String> getTableConditions() {
        return TABLE_CONDITION_THREAD_LOCAL.get();
    }

    public static Map<String, String> getViewConditions() {
        return VIEW_CONDITION_THREAD_LOCAL.get();
    }

    public static void clearTableConditions() {
        TABLE_CONDITION_THREAD_LOCAL.remove();
    }

    public static void clearViewConditions() {
        VIEW_CONDITION_THREAD_LOCAL.remove();
    }

    public static void clearConditions() {
        clearTableConditions();
        clearViewConditions();
    }

}
