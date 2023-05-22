package cn.addenda.businesseasy.jdbc.interceptor.dynamicsql;

import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.*;
import cn.addenda.businesseasy.jdbc.visitor.additem.InsertSelectAddItemMode;
import cn.addenda.businesseasy.jdbc.visitor.additem.Item;
import cn.addenda.businesseasy.jdbc.visitor.additem.UpdateItemMode;
import cn.addenda.businesseasy.util.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @author addenda
 * @since 2023/4/30 16:30
 */
@Slf4j
public class DynamicSQLInterceptor extends ConnectionPrepareStatementInterceptor {

    private final DynamicSQLAssembler dynamicSQLAssembler;

    private final boolean defaultJoinUseSubQuery;
    private final boolean defaultDuplicateKeyUpdate;
    private final InsertSelectAddItemMode defaultInsertSelectAddItemMode;
    private final UpdateItemMode defaultUpdateItemMode;

    public DynamicSQLInterceptor(DynamicSQLAssembler dynamicSQLAssembler, boolean duplicateKeyUpdate,
                                 InsertSelectAddItemMode insertSelectAddItemMode,
                                 UpdateItemMode updateItemMode, boolean useSubQuery) {
        this.dynamicSQLAssembler = dynamicSQLAssembler;
        this.defaultDuplicateKeyUpdate = duplicateKeyUpdate;
        this.defaultInsertSelectAddItemMode = insertSelectAddItemMode == null ? InsertSelectAddItemMode.ITEM : insertSelectAddItemMode;
        this.defaultUpdateItemMode = updateItemMode == null ? UpdateItemMode.NOT_NULL : updateItemMode;
        this.defaultJoinUseSubQuery = useSubQuery;
    }

    public DynamicSQLInterceptor() {
        this(new DruidDynamicSQLAssembler(), false,
                InsertSelectAddItemMode.ITEM, UpdateItemMode.NOT_NULL, false);
    }

    protected String process(String sql) {
        Map<String, List<Map.Entry<String, String>>> conditionMap = DynamicSQLContext.getConditionMap();
        Map<String, List<Map.Entry<String, Item>>> itemMap = DynamicSQLContext.getItemMap();

        if (conditionMap == null && itemMap == null) {
            return sql;
        }
        log.debug("Dynamic Condition, before sql rewriting: [{}].", removeEnter(sql));
        String newSql;
        try {
            newSql = doProcess(removeEnter(sql), conditionMap, itemMap);
        } catch (Throwable throwable) {
            String msg = String.format("拼装动态条件时出错，SQL：[%s]，conditionMap: [%s]，itemMap：[%s]。", removeEnter(sql), conditionMap, itemMap);
            throw new DynamicSQLException(msg, ExceptionUtil.unwrapThrowable(throwable));
        }

        log.debug("Dynamic Condition, after sql rewriting: [{}].", newSql);
        return newSql;
    }

    private String doProcess(String sql, Map<String, List<Map.Entry<String, String>>> conditionMap, Map<String, List<Map.Entry<String, Item>>> itemMap) {
        String newSql = sql;

        // condition 过滤条件
        if (conditionMap != null && !conditionMap.isEmpty()) {
            for (Map.Entry<String, List<Map.Entry<String, String>>> tableEntry : conditionMap.entrySet()) {
                String tableName = tableEntry.getKey();
                if (DynamicSQLContext.ALL_TABLE.equals(tableName)) {
                    tableName = null;
                }
                for (Map.Entry<String, String> operationEntry : tableEntry.getValue()) {
                    String operation = operationEntry.getKey();
                    String condition = operationEntry.getValue();
                    if (DynamicSQLContext.TABLE_ADD_JOIN_CONDITION.equals(operation) && !JdbcSQLUtils.isInsert(newSql)) {
                        Boolean useSubQuery =
                                JdbcSQLUtils.getOrDefault(DynamicSQLContext.getJoinUseSubQuery(), defaultJoinUseSubQuery);
                        newSql = dynamicSQLAssembler.tableAddJoinCondition(newSql, tableName, condition, useSubQuery);
                    } else if (DynamicSQLContext.VIEW_ADD_JOIN_CONDITION.equals(operation) && !JdbcSQLUtils.isInsert(newSql)) {
                        Boolean useSubQuery =
                                JdbcSQLUtils.getOrDefault(DynamicSQLContext.getJoinUseSubQuery(), defaultJoinUseSubQuery);
                        newSql = dynamicSQLAssembler.viewAddJoinCondition(newSql, tableName, condition, useSubQuery);
                    } else if (DynamicSQLContext.TABLE_ADD_WHERE_CONDITION.equals(operation) && !JdbcSQLUtils.isInsert(newSql)) {
                        newSql = dynamicSQLAssembler.tableAddWhereCondition(newSql, tableName, condition);
                    } else if (DynamicSQLContext.VIEW_ADD_WHERE_CONDITION.equals(operation) && !JdbcSQLUtils.isInsert(newSql)) {
                        newSql = dynamicSQLAssembler.viewAddWhereCondition(newSql, tableName, condition);
                    } else {
                        String msg = String.format("不支持的SQL添加条件操作类型：[%s]，SQL：[%s]。", operation, removeEnter(sql));
                        throw new UnsupportedOperationException(msg);
                    }
                }
            }
        }

        // item 过滤条件
        if (itemMap != null && !itemMap.isEmpty()) {
            for (Map.Entry<String, List<Map.Entry<String, Item>>> tableEntry : itemMap.entrySet()) {
                String tableName = tableEntry.getKey();
                if (DynamicSQLContext.ALL_TABLE.equals(tableName)) {
                    tableName = null;
                }
                for (Map.Entry<String, Item> operationEntry : tableEntry.getValue()) {
                    String operation = operationEntry.getKey();
                    Item item = operationEntry.getValue();
                    UpdateItemMode updateItemMode =
                            JdbcSQLUtils.getOrDefault(DynamicSQLContext.getUpdateItemMode(), defaultUpdateItemMode);
                    if (DynamicSQLContext.INSERT_ADD_ITEM.equals(operation) && JdbcSQLUtils.isInsert(newSql)) {
                        Boolean duplicateKeyUpdate =
                                JdbcSQLUtils.getOrDefault(DynamicSQLContext.getDuplicateKeyUpdate(), defaultDuplicateKeyUpdate);
                        InsertSelectAddItemMode insertSelectAddItemMode =
                                JdbcSQLUtils.getOrDefault(DynamicSQLContext.getInsertSelectAddItemMode(), defaultInsertSelectAddItemMode);
                        newSql = dynamicSQLAssembler.insertAddItem(newSql, tableName, item, insertSelectAddItemMode, duplicateKeyUpdate, updateItemMode);
                    } else if (DynamicSQLContext.UPDATE_ADD_ITEM.equals(operation) && JdbcSQLUtils.isUpdate(newSql)) {
                        newSql = dynamicSQLAssembler.updateAddItem(newSql, tableName, item, updateItemMode);
                    } else {
                        String msg = String.format("不支持的SQL添加item作类型：[%s]，SQL：[%s]。", operation, removeEnter(sql));
                        throw new UnsupportedOperationException(msg);
                    }
                }

            }
        }

        return newSql;
    }

}
