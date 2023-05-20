package cn.addenda.businesseasy.jdbc.interceptor.dynamicsql;

import cn.addenda.businesseasy.jdbc.interceptor.*;
import cn.addenda.businesseasy.util.BEArrayUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;

/**
 * @author addenda
 * @since 2023/4/30 16:56
 */
public class DruidDynamicSQLAssembler implements DynamicSQLAssembler {

    @Override
    public String tableAddJoinCondition(
            String sql, String tableName, String condition, boolean useSubQuery) {
        return DruidSQLUtils.statementMerge(sql, sqlStatement -> {
            sqlStatement.accept(new TableAddJoinConditionVisitor(tableName, condition, useSubQuery));
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    @Override
    public String viewAddJoinCondition(
            String sql, String tableName, String condition, boolean useSubQuery) {
        return DruidSQLUtils.statementMerge(sql, sqlStatement -> {
            sqlStatement.accept(new ViewAddJoinConditionVisitor(tableName, condition, useSubQuery));
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    @Override
    public String tableAddWhereCondition(String sql, String tableName, String condition) {
        return DruidSQLUtils.statementMerge(sql, sqlStatement -> {
            sqlStatement.accept(new TableAddWhereConditionVisitor(tableName, condition));
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    @Override
    public String viewAddWhereCondition(String sql, String tableName, String condition) {
        return DruidSQLUtils.statementMerge(sql, sqlStatement -> {
            sqlStatement.accept(new ViewAddWhereConditionVisitor(tableName, condition));
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    @Override
    public String insertAddItem(String sql, String tableName, Item item, InsertSelectAddItemMode insertSelectAddItemMode,
                                boolean duplicateKeyUpdate, UpdateItemMode updateItemMode) {
        return DruidSQLUtils.statementMerge(sql, sqlStatement -> {
            new InsertAddItemVisitor((MySqlInsertStatement) sqlStatement, tableName == null ? null : BEArrayUtils.asArrayList(tableName), null,
                    item, true, insertSelectAddItemMode, duplicateKeyUpdate, updateItemMode).visit();
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    @Override
    public String updateAddItem(String sql, String tableName, Item item, UpdateItemMode updateItemMode) {
        return DruidSQLUtils.statementMerge(sql, sqlStatement -> {
            new UpdateAddItemVisitor((MySqlUpdateStatement) sqlStatement, tableName == null ? null : BEArrayUtils.asArrayList(tableName), null,
                    item, true, updateItemMode).visit();
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        });
    }

    private void checkIdentifierExists(SQLStatement sql, Item item, String tableName) {
        IdentifierExistsVisitor identifierExistsVisitor = new InsertOrUpdateItemNameIdentifierExistsVisitor(
                sql, item.getItemName(), BEArrayUtils.asArrayList(tableName), null, false);
        identifierExistsVisitor.visit();
        if (identifierExistsVisitor.isExists()) {
            String msg = String.format("itemName已存在，SQL: [%s]，itemName：[%s]，itemValue：[%s]。",
                    DruidSQLUtils.toLowerCaseSQL(sql), item.getItemName(), DruidSQLUtils.toLowerCaseSQL(DruidSQLUtils.objectToSQLExpr(item.getItemValue())));
            throw new DynamicSQLException(msg);
        }
    }

}
