package cn.addenda.businesseasy.jdbc.interceptor.dynamicsql;

import cn.addenda.businesseasy.jdbc.interceptor.*;
import cn.addenda.businesseasy.util.BEArrayUtils;

/**
 * @author addenda
 * @since 2023/4/30 16:56
 */
public class DruidDynamicSQLAssembler extends AbstractDruidSqlRewriter implements DynamicSQLAssembler {

    private final boolean useSubQuery;

    public DruidDynamicSQLAssembler(boolean useSubQuery) {
        this.useSubQuery = useSubQuery;
    }

    public DruidDynamicSQLAssembler() {
        this.useSubQuery = false;
    }

    @Override
    public String tableAddJoinCondition(
            String sql, String tableName, String condition) {
        return singleRewriteSql(sql, sqlStatement -> {
            sqlStatement.accept(new TableAddJoinConditionVisitor(tableName, condition, useSubQuery));
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    @Override
    public String viewAddJoinCondition(
            String sql, String tableName, String condition) {
        return singleRewriteSql(sql, sqlStatement -> {
            sqlStatement.accept(new ViewAddJoinConditionVisitor(tableName, condition, useSubQuery));
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    @Override
    public String tableAddWhereCondition(String sql, String tableName, String condition) {
        return singleRewriteSql(sql, sqlStatement -> {
            sqlStatement.accept(new TableAddWhereConditionVisitor(tableName, condition));
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    @Override
    public String viewAddWhereCondition(String sql, String tableName, String condition) {
        return singleRewriteSql(sql, sqlStatement -> {
            sqlStatement.accept(new ViewAddWhereConditionVisitor(tableName, condition));
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    @Override
    public String insertAddItem(String sql, String tableName, String itemName, Object itemValue) {
        return baseAddItem(sql, tableName, itemName, itemValue);
    }

    @Override
    public String updateAddItem(String sql, String tableName, String itemName, Object itemValue) {
        return baseAddItem(sql, tableName, itemName, itemValue);
    }

    private String baseAddItem(String sql, String tableName, String itemName, Object itemValue) {
        return singleRewriteSql(sql, sqlStatement -> {
            IdentifierExistsVisitor identifierExistsVisitor = new InsertOrUpdateItemNameIdentifierExistsVisitor(
                    sql, itemName, BEArrayUtils.asArrayList(tableName), null, false);
            identifierExistsVisitor.visit();
            if (identifierExistsVisitor.isExists()) {
                String msg = String.format("itemName已存在，SQL: [%s]，itemName：[%s]，itemValue：[%s]。",
                        sql, itemName, DruidSQLUtils.toLowerCaseSQL(DruidSQLUtils.objectToSQLExpr(itemName)));
                throw new DynamicSQLException(msg);
            }
            sqlStatement.accept(new ViewToTableVisitor());
            sqlStatement.accept(new InsertOrUpdateAddItemVisitor(tableName, itemName, itemValue));
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

}
