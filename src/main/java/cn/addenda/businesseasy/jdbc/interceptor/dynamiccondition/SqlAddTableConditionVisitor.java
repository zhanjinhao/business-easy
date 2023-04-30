package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;

import java.util.Objects;

/**
 * @author addenda
 * @since 2023/4/28 10:05
 */
public class SqlAddTableConditionVisitor extends MySqlASTVisitorAdapter {

    private final String tableName;

    private final String condition;

    private boolean useWhereConditionAsPossible = false;

    private boolean tableNameEqualCaseInsensitive = true;

    public SqlAddTableConditionVisitor(String condition) {
        this.tableName = null;
        this.condition = condition;
    }

    public SqlAddTableConditionVisitor(String tableName, String condition) {
        this.tableName = tableName;
        this.condition = condition;
    }

    public SqlAddTableConditionVisitor(String tableName, String condition, boolean useWhereConditionAsPossible) {
        this.tableName = tableName;
        this.condition = condition;
        this.useWhereConditionAsPossible = useWhereConditionAsPossible;
    }

    public SqlAddTableConditionVisitor(
            String tableName, String condition, boolean useWhereConditionAsPossible, boolean tableNameEqualCaseInsensitive) {
        this.tableName = tableName;
        this.condition = condition;
        this.useWhereConditionAsPossible = useWhereConditionAsPossible;
        this.tableNameEqualCaseInsensitive = tableNameEqualCaseInsensitive;
    }

    @Override
    public void endVisit(SQLExprTableSource x) {
        String alias = x.getAlias();
        String _tableName = x.getTableName();
        SQLObject parent = x.getParent();
        if (tableName != null && !tableNameEqual(getTableName(_tableName, alias), tableName)) {
            return;
        }

        if (parent instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) parent;
            if (useWhereConditionAsPossible) {
                sqlSelectQueryBlock.setWhere(newWhere(sqlSelectQueryBlock.getWhere()));
            } else {
                sqlSelectQueryBlock.setFrom(newFrom(_tableName, alias));
            }
        } else if (parent instanceof SQLJoinTableSource) {
            SQLJoinTableSource sqlJoinTableSource = (SQLJoinTableSource) parent;
            if (x == sqlJoinTableSource.getLeft()) {
                sqlJoinTableSource.setLeft(newFrom(_tableName, alias));
            } else if (x == sqlJoinTableSource.getRight()) {
                sqlJoinTableSource.setRight(newFrom(_tableName, alias));
            }
        } else if (parent instanceof MySqlDeleteStatement) {
            MySqlDeleteStatement mySqlDeleteStatement = (MySqlDeleteStatement) parent;
            mySqlDeleteStatement.setWhere(newWhere(mySqlDeleteStatement.getWhere()));
        } else if (parent instanceof MySqlUpdateStatement) {
            MySqlUpdateStatement mySqlUpdateStatement = (MySqlUpdateStatement) parent;
            mySqlUpdateStatement.setWhere(newWhere(mySqlUpdateStatement.getWhere()));
        }

    }

    private boolean tableNameEqual(String origin1, String origin2) {
        if (tableNameEqualCaseInsensitive) {
            return origin1.equalsIgnoreCase(origin2);
        }

        return origin1.equals(origin2);
    }

    protected String getTableName(String tableName, String alias) {
        return tableName;
    }

    private SQLTableSource newFrom(String tableName, String alias) {
        SQLSelectStatement sqlStatement = (SQLSelectStatement) SQLUtils.parseStatements(
                "select * from (select * from " + tableName + " where " + condition + ")", dbType).get(0);
        SQLTableSource from = ((SQLSelectQueryBlock) sqlStatement.getSelect().getQuery()).getFrom();
        if (alias == null) {
            from.setAlias(tableName);
        } else {
            from.setAlias(alias);
        }
        return from;
    }

    private SQLExpr newWhere(SQLExpr where) {
        return SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd,
                SQLUtils.toSQLExpr(condition, DbType.mysql), false, where);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlAddTableConditionVisitor that = (SqlAddTableConditionVisitor) o;
        return useWhereConditionAsPossible == that.useWhereConditionAsPossible &&
                tableNameEqualCaseInsensitive == that.tableNameEqualCaseInsensitive &&
                Objects.equals(tableName, that.tableName) &&
                Objects.equals(condition, that.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, condition,
                useWhereConditionAsPossible, tableNameEqualCaseInsensitive);
    }

    @Override
    public String toString() {
        return "SqlAddTableConditionVisitor{" +
                "tableName='" + tableName + '\'' +
                ", condition='" + condition + '\'' +
                ", useWhereConditionAsPossible=" + useWhereConditionAsPossible +
                ", tableNameEqualCaseInsensitive=" + tableNameEqualCaseInsensitive +
                "} " + super.toString();
    }
}
