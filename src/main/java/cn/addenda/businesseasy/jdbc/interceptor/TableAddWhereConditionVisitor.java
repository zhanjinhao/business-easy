package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 先连接，再应用过滤条件。
 *
 * @author addenda
 * @since 2023/5/11 12:30
 */
public class TableAddWhereConditionVisitor extends AbstractAddConditionVisitor {

    public TableAddWhereConditionVisitor(String condition) {
        super(null, null, condition);
    }

    public TableAddWhereConditionVisitor(String tableName, String condition) {
        super(new ArrayList<>(Collections.singletonList(tableName)), null, condition);
    }

    public TableAddWhereConditionVisitor(
        List<String> included, List<String> notIncluded, String condition) {
        super(included, notIncluded, condition);
    }

    @Override
    public void endVisit(SQLJoinTableSource x) {
        SQLTableSource left = x.getLeft();
        List<String> leftTableNameList = getTableNameList(left);
        List<String> leftAliasList = getAliasList(left);

        List<String> tableNameList = new ArrayList<>();
        List<String> aliasList = new ArrayList<>();
        if (leftTableNameList != null) {
            tableNameList.addAll(leftAliasList);
            aliasList.addAll(leftAliasList);
            clear(left);
        }

        SQLTableSource right = x.getRight();
        List<String> rightTableNameList = getTableNameList(right);
        List<String> rightAliasList = getAliasList(right);
        if (rightTableNameList != null) {
            tableNameList.addAll(rightTableNameList);
            aliasList.addAll(rightAliasList);
            clear(right);
        }

        if (!tableNameList.isEmpty()) {
            x.putAttribute(TABLE_NAME_KEY, tableNameList);
            x.putAttribute(ALIAS_KEY, aliasList);
        }
    }

    @Override
    public void endVisit(MySqlSelectQueryBlock x) {
        SQLTableSource from = x.getFrom();
        List<String> tableNameList = getTableNameList(from);
        List<String> aliasList = getAliasList(from);
        for (int i = 0; i < tableNameList.size(); i++) {
            String aTableName = tableNameList.get(i);
            String aAlias = aliasList.get(i);
            if (aTableName != null) {
                x.setWhere(newWhere(x.getWhere(), aTableName, aAlias));
                clear(from);
            }
        }
    }

    @Override
    public void endVisit(MySqlUpdateStatement x) {
        SQLTableSource from = x.getTableSource();
        List<String> tableNameList = getTableNameList(from);
        List<String> aliasList = getAliasList(from);
        for (int i = 0; i < tableNameList.size(); i++) {
            String aTableName = tableNameList.get(i);
            String aAlias = aliasList.get(i);
            if (aTableName != null) {
                x.setWhere(newWhere(x.getWhere(), aTableName, aAlias));
                clear(from);
            }
        }
    }

}
