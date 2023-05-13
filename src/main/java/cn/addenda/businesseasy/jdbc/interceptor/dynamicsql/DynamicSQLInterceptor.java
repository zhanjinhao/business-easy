package cn.addenda.businesseasy.jdbc.interceptor.dynamicsql;

import cn.addenda.businesseasy.jdbc.interceptor.ConnectionPrepareStatementInterceptor;
import cn.addenda.businesseasy.jdbc.interceptor.Item;
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

    private DynamicSQLAssembler dynamicSQLAssembler;

    public DynamicSQLInterceptor(DynamicSQLAssembler dynamicSQLAssembler) {
        this.dynamicSQLAssembler = dynamicSQLAssembler;
    }

    public DynamicSQLInterceptor() {
        this.dynamicSQLAssembler = new DruidDynamicSQLAssembler();
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
                    if (DynamicSQLContext.TABLE_ADD_JOIN_CONDITION.equals(operation)) {
                        newSql = dynamicSQLAssembler.tableAddJoinCondition(newSql, tableName, condition);
                    } else if (DynamicSQLContext.VIEW_ADD_JOIN_CONDITION.equals(operation)) {
                        newSql = dynamicSQLAssembler.viewAddJoinCondition(newSql, tableName, condition);
                    } else if (DynamicSQLContext.TABLE_ADD_WHERE_CONDITION.equals(operation)) {
                        newSql = dynamicSQLAssembler.tableAddWhereCondition(newSql, tableName, condition);
                    } else if (DynamicSQLContext.VIEW_ADD_WHERE_CONDITION.equals(operation)) {
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
                    if (DynamicSQLContext.INSERT_ADD_ITEM.equals(operation)) {
                        newSql = dynamicSQLAssembler.insertAddItem(newSql, tableName, item);
                    } else if (DynamicSQLContext.UPDATE_ADD_ITEM.equals(operation)) {
                        newSql = dynamicSQLAssembler.updateAddItem(newSql, tableName, item);
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
