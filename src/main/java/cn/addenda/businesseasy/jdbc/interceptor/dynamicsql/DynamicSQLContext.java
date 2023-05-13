package cn.addenda.businesseasy.jdbc.interceptor.dynamicsql;

import cn.addenda.businesseasy.jdbc.interceptor.Item;

import java.util.*;

/**
 * @author addenda
 * @since 2022/11/26 20:56
 */
public class DynamicSQLContext {

    private DynamicSQLContext() {
    }

    public static final String ALL_TABLE = "ALL@ALL";

    public static final String TABLE_ADD_JOIN_CONDITION = "TABLE_ADD_JOIN_CONDITION";
    public static final String VIEW_ADD_JOIN_CONDITION = "VIEW_ADD_JOIN_CONDITION";
    public static final String TABLE_ADD_WHERE_CONDITION = "TABLE_ADD_WHERE_CONDITION";
    public static final String VIEW_ADD_WHERE_CONDITION = "VIEW_ADD_WHERE_CONDITION";
    public static final String INSERT_ADD_ITEM = "ITEM_ADD_ITEM";
    public static final String UPDATE_ADD_ITEM = "UPDATE_ADD_ITEM";

    private static final ThreadLocal<Map<String, List<Map.Entry<String, String>>>> CONDITION_THREAD_LOCAL = ThreadLocal.withInitial(LinkedHashMap::new);
    private static final ThreadLocal<Map<String, List<Map.Entry<String, Item>>>> ITEM_THREAD_LOCAL = ThreadLocal.withInitial(LinkedHashMap::new);

    public static void tableAddJoinCondition(String tableName, String condition) {
        addCondition(TABLE_ADD_JOIN_CONDITION, tableName, condition);
    }

    public static void viewAddJoinCondition(String viewName, String condition) {
        addCondition(VIEW_ADD_JOIN_CONDITION, viewName, condition);
    }

    public static void tableAddWhereCondition(String tableName, String condition) {
        addCondition(TABLE_ADD_WHERE_CONDITION, tableName, condition);
    }

    public static void viewAddWhereCondition(String viewName, String condition) {
        addCondition(VIEW_ADD_WHERE_CONDITION, viewName, condition);
    }

    private static void addCondition(String operation, String name, String condition) {
        Map<String, List<Map.Entry<String, String>>> stringListMap = CONDITION_THREAD_LOCAL.get();
        if (name == null) {
            name = DynamicSQLContext.ALL_TABLE;
        }
        List<Map.Entry<String, String>> entries = stringListMap.computeIfAbsent(name, k -> new ArrayList<>());
        ConditionEntry conditionEntry = new ConditionEntry(operation, condition);
        entries.add(conditionEntry);
    }

    public static void insertAddItem(String tableName, String itemName, Object itemValue) {
        addItem(INSERT_ADD_ITEM, tableName, itemName, itemValue);
    }

    public static void updateAddItem(String tableName, String itemName, Object itemValue) {
        addItem(UPDATE_ADD_ITEM, tableName, itemName, itemValue);
    }

    private static void addItem(String operation, String name, String itemName, Object itemValue) {
        Map<String, List<Map.Entry<String, Item>>> stringListMap = ITEM_THREAD_LOCAL.get();
        if (name == null) {
            name = DynamicSQLContext.ALL_TABLE;
        }
        List<Map.Entry<String, Item>> entries = stringListMap.computeIfAbsent(name, k -> new ArrayList<>());
        ItemEntry itemEntry = new ItemEntry(operation, new Item(itemName, itemValue));
        entries.add(itemEntry);
    }

    public static Map<String, List<Map.Entry<String, String>>> getConditionMap() {
        return CONDITION_THREAD_LOCAL.get();
    }

    public static Map<String, List<Map.Entry<String, Item>>> getItemMap() {
        return ITEM_THREAD_LOCAL.get();
    }

    public static void clearCondition() {
        CONDITION_THREAD_LOCAL.remove();
    }

    public static void clearItem() {
        ITEM_THREAD_LOCAL.remove();
    }

    public static void clear() {
        clearCondition();
        clearItem();
    }

    private static class ConditionEntry implements Map.Entry<String, String> {
        private final String operation;
        private String condition;

        public ConditionEntry(String operation, String condition) {
            this.operation = operation;
            this.condition = condition;
        }

        @Override
        public String getKey() {
            return operation;
        }

        @Override
        public String getValue() {
            return condition;
        }

        @Override
        public String setValue(String newValue) {
            String oldValue = condition;
            condition = newValue;
            return oldValue;
        }
    }

    private static class ItemEntry implements Map.Entry<String, Item> {
        private final String operation;
        private Item item;

        public ItemEntry(String operation, Item item) {
            this.operation = operation;
            this.item = item;
        }

        @Override
        public String getKey() {
            return operation;
        }

        @Override
        public Item getValue() {
            return item;
        }

        @Override
        public Item setValue(Item newValue) {
            Item oldValue = item;
            item = newValue;
            return oldValue;
        }
    }

}
