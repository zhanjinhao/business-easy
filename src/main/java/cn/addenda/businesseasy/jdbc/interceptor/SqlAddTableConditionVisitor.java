package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author addenda
 * @since 2023/4/28 10:05
 */
public class SqlAddTableConditionVisitor extends MySqlASTVisitorAdapter {

    private final List<String> included;
    private final List<String> notIncluded;

    private final String condition;

    private final boolean useWhereConditionAsPossible;

    private static final String TABLE_NAME_KEY = "tableNameKey";
    private static final String ALIAS_KEY = "aliasKey";

    public SqlAddTableConditionVisitor(String condition) {
        this.included = null;
        this.notIncluded = null;
        this.condition = condition;
        this.useWhereConditionAsPossible = false;
    }

    public SqlAddTableConditionVisitor(String tableName, String condition) {
        this.included = new ArrayList<>();
        included.add(tableName);
        this.notIncluded = null;
        this.condition = condition;
        this.useWhereConditionAsPossible = false;
    }

    public SqlAddTableConditionVisitor(String tableName, String condition, boolean useWhereConditionAsPossible) {
        this.included = new ArrayList<>();
        included.add(tableName);
        this.notIncluded = null;
        this.condition = condition;
        this.useWhereConditionAsPossible = useWhereConditionAsPossible;
    }

    public SqlAddTableConditionVisitor(
            List<String> included, List<String> notIncluded, String condition, boolean useWhereConditionAsPossible) {
        this.included = included;
        this.notIncluded = notIncluded;
        this.condition = condition;
        this.useWhereConditionAsPossible = useWhereConditionAsPossible;
    }

    @Override
    public void endVisit(SQLExprTableSource x) {
        String aAlias = x.getAlias();
        String aTableName = x.getTableName();
        if (included != null && !JdbcSQLUtils.include(getTableName(aTableName, aAlias), included, notIncluded)) {
            return;
        }
        x.putAttribute(TABLE_NAME_KEY, aTableName);
        x.putAttribute(ALIAS_KEY, aAlias);
    }

    @Override
    public void endVisit(SQLJoinTableSource x) {
        SQLTableSource left = x.getLeft();
        String leftTableName = getTableName(left);
        String leftAlias = getAlias(left);

        if (leftTableName != null) {
            if (useWhereConditionAsPossible) {
                x.setCondition(newWhere(x.getCondition(), leftTableName, leftAlias));
            } else {
                x.setLeft(newFrom(leftTableName, leftAlias));
            }
        }

        SQLTableSource right = x.getRight();
        String rightTableName = getTableName(right);
        String rightAlias = getAlias(right);
        if (rightTableName != null) {
            if (useWhereConditionAsPossible) {
                x.setCondition(newWhere(x.getCondition(), rightTableName, rightAlias));
            } else {
                x.setRight(newFrom(rightTableName, rightAlias));
            }
        }
    }

    @Override
    public void endVisit(MySqlSelectQueryBlock x) {
        SQLTableSource from = x.getFrom();
        String aTableName = getTableName(from);
        String aAlias = getAlias(from);
        if (aTableName != null) {
            if (useWhereConditionAsPossible) {
                x.setWhere(newWhere(x.getWhere(), aTableName, aAlias));
            } else {
                x.setFrom(newFrom(aTableName, aAlias));
            }
        }
    }

    @Override
    public void endVisit(MySqlDeleteStatement x) {
        SQLTableSource tableSource = x.getTableSource();
        String aTableName = getTableName(tableSource);
        String aAlias = getAlias(tableSource);
        if (aTableName != null) {
            x.setWhere(newWhere(x.getWhere(), aTableName, aAlias));
        }
    }

    @Override
    public void endVisit(MySqlUpdateStatement x) {
        SQLTableSource tableSource = x.getTableSource();
        String aTableName = getTableName(tableSource);
        String aAlias = getAlias(tableSource);
        if (aTableName != null) {
            x.setWhere(newWhere(x.getWhere(), aTableName, aAlias));
        }
    }

    protected String getTableName(String tableName, String alias) {
        return tableName;
    }

    private SQLTableSource newFrom(String tableName, String alias) {
        String view = alias == null ? tableName : alias;
        SQLSelectStatement sqlStatement = (SQLSelectStatement) SQLUtils.parseStatements(
                "select * from (select * from " + tableName + " where " + condition + ")", dbType).get(0);
        SQLTableSource from = ((SQLSelectQueryBlock) sqlStatement.getSelect().getQuery()).getFrom();
        from.setAlias(view);
        return from;
    }

    private SQLExpr newWhere(SQLExpr where, String tableName, String alias) {
        String view = alias == null ? tableName : alias;
        return SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd,
                SQLUtils.toSQLExpr(view + "." + condition, DbType.mysql), false, where);
    }

    private String getTableName(SQLObject sqlObject) {
        return (String) sqlObject.getAttribute(TABLE_NAME_KEY);
    }

    private String getAlias(SQLObject sqlObject) {
        return (String) sqlObject.getAttribute(ALIAS_KEY);
    }

}
