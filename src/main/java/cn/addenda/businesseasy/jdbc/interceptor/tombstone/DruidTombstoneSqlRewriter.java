package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.AbstractDruidSqlRewriter;
import cn.addenda.businesseasy.jdbc.interceptor.DruidSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.SqlAddTableConditionVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @since 2023/4/30 19:42
 */
@Slf4j
public class DruidTombstoneSqlRewriter extends AbstractDruidSqlRewriter implements TombstoneSqlRewriter {

    private static final String TOMBSTONE_NAME = "if_del";
    private static final Short TOMBSTONE_VALUE = 0;

    private boolean useWhereConditionAsPossible = true;

    private boolean tableNameEqualCaseInsensitive = true;

    /**
     * 逻辑删除的表
     */
    private final List<String> included;

    /**
     * 非逻辑删除的表
     */
    private final List<String> notIncluded = new ArrayList<>(Collections.singletonList("dual"));

    public DruidTombstoneSqlRewriter(List<String> included,
                                     boolean useWhereConditionAsPossible, boolean tableNameEqualCaseInsensitive) {
        this(included);
        this.useWhereConditionAsPossible = useWhereConditionAsPossible;
        this.tableNameEqualCaseInsensitive = tableNameEqualCaseInsensitive;
    }

    public DruidTombstoneSqlRewriter(List<String> included) {
        this.included = included;
        if (included == null) {
            log.warn("未声明逻辑删除的表集合，所有的表都会进行逻辑删除改写！");
        }
    }

    public DruidTombstoneSqlRewriter() {
        this(null);
    }

    @Override
    public String rewriteInsertSql(String sql) {
        return singleRewriteSql(sql, this::doRewriteInsertSql);
    }

    private String doRewriteInsertSql(SQLStatement sqlStatement) {
        MySqlInsertStatement insertStatement = (MySqlInsertStatement) sqlStatement;
        SQLName tableName = insertStatement.getTableName();
        if (!JdbcSQLUtils.include(tableName.toString(), included, notIncluded)) {
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        }
        List<SQLExpr> columns = insertStatement.getColumns();
        for (SQLExpr sqlExpr : columns) {
            if (TOMBSTONE_NAME.equalsIgnoreCase(sqlExpr.toString())) {
                return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
            }
        }
        columns.add(SQLUtils.toSQLExpr(TOMBSTONE_NAME));
        List<SQLInsertStatement.ValuesClause> valuesList = insertStatement.getValuesList();
        for (SQLInsertStatement.ValuesClause value : valuesList) {
            value.addValue(new SQLIntegerExpr(TOMBSTONE_VALUE));
        }
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    @Override
    public String rewriteDeleteSql(String sql) {
        return singleRewriteSql(sql, this::doRewriteDeleteSql);
    }

    private String doRewriteDeleteSql(SQLStatement sqlStatement) {
        MySqlDeleteStatement mySqlDeleteStatement = (MySqlDeleteStatement) sqlStatement;
        SQLName tableName = mySqlDeleteStatement.getTableName();
        if (!JdbcSQLUtils.include(tableName.toString(), included, notIncluded)) {
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        }
        SQLExpr where = mySqlDeleteStatement.getWhere();
        where = SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd,
                SQLUtils.toSQLExpr(TOMBSTONE_NAME + "=" + TOMBSTONE_VALUE, DbType.mysql), false, where);
        return "delete from " + mySqlDeleteStatement.getTableName() + " where " + DruidSQLUtils.toLowerCaseSQL(where);
    }

    @Override
    public String rewriteSelectSql(String sql) {
        return singleRewriteSql(sql, this::doRewriteSelectSql);
    }

    private String doRewriteSelectSql(SQLStatement sqlStatement) {
        SQLSelectStatement sqlSelectStatement = (SQLSelectStatement) sqlStatement;
        SchemaStatVisitor schemaStatVisitor = new SchemaStatVisitor();
        sqlSelectStatement.accept(schemaStatVisitor);
        Map<TableStat.Name, TableStat> tables = schemaStatVisitor.getTables();
        Set<String> collect = tables.keySet().stream().map(TableStat.Name::getName).collect(Collectors.toSet());
        for (String table : collect) {
            if (JdbcSQLUtils.include(table, included, notIncluded)) {
                sqlSelectStatement.accept(new SqlAddTableConditionVisitor(
                        table, TOMBSTONE_NAME + "=" + TOMBSTONE_VALUE, useWhereConditionAsPossible));
            }
        }
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    @Override
    public String rewriteUpdateSql(String sql) {
        return singleRewriteSql(sql, this::doRewriteUpdateSql);
    }

    private String doRewriteUpdateSql(SQLStatement sqlStatement) {
        MySqlUpdateStatement mySqlUpdateStatement = (MySqlUpdateStatement) sqlStatement;
        SQLName tableName = mySqlUpdateStatement.getTableName();
        if (!JdbcSQLUtils.include(tableName.toString(), included, notIncluded)) {
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        }
        List<SQLUpdateSetItem> items = mySqlUpdateStatement.getItems();
        for (SQLUpdateSetItem item : items) {
            SQLExpr column = item.getColumn();
            if (TOMBSTONE_NAME.equals(column.toString())) {
                return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
            }
        }
        SQLUpdateSetItem item = new SQLUpdateSetItem();
        item.setColumn(SQLUtils.toSQLExpr(TOMBSTONE_NAME));
        item.setValue(new SQLIntegerExpr(TOMBSTONE_VALUE));
        items.add(item);
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

}
