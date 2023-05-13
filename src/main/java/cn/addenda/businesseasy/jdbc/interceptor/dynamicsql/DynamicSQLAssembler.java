package cn.addenda.businesseasy.jdbc.interceptor.dynamicsql;

import cn.addenda.businesseasy.jdbc.interceptor.Item;

/**
 * @author addenda
 * @since 2023/4/30 16:56
 */
public interface DynamicSQLAssembler {

    String tableAddJoinCondition(String sql, String tableName, String condition);

    String viewAddJoinCondition(String sql, String tableName, String condition);

    String tableAddWhereCondition(String sql, String tableName, String condition);

    String viewAddWhereCondition(String sql, String tableName, String condition);

    String insertAddItem(String sql, String tableName, Item item);

    String updateAddItem(String sql, String tableName, Item item);

}
