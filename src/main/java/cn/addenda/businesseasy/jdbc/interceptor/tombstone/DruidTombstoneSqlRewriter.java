package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.AbstractDruidSqlRewriter;
import cn.addenda.businesseasy.jdbc.interceptor.DruidSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition.SqlAddTableConditionVisitor;
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

    private String tombstoneName = "if_del";
    private Short tombstoneValue = 0;

    private boolean useWhereConditionAsPossible = true;

    private boolean tableNameEqualCaseInsensitive = true;

    /**
     * 逻辑删除的表
     */
    private List<String> tombstoneTableNameList;

    /**
     * 非逻辑删除的表
     */
    private List<String> unTombstoneTableNameList = new ArrayList<>(Arrays.asList("dual"));

    public DruidTombstoneSqlRewriter(List<String> tombstoneTableNameList,
                                     boolean useWhereConditionAsPossible, boolean tableNameEqualCaseInsensitive) {
        this(tombstoneTableNameList);
        this.useWhereConditionAsPossible = useWhereConditionAsPossible;
        this.tableNameEqualCaseInsensitive = tableNameEqualCaseInsensitive;
    }

    public DruidTombstoneSqlRewriter(List<String> tombstoneTableNameList) {
        if (tombstoneTableNameList != null) {
            this.tombstoneTableNameList = new ArrayList<>();
            this.tombstoneTableNameList.addAll(tombstoneTableNameList);
        } else {
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
        if (!JdbcSQLUtils.contains(tableName.toString(), tombstoneTableNameList, unTombstoneTableNameList)) {
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        }
        List<SQLExpr> columns = insertStatement.getColumns();
        for (SQLExpr sqlExpr : columns) {
            if (tombstoneName.equalsIgnoreCase(sqlExpr.toString())) {
                return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
            }
        }
        columns.add(SQLUtils.toSQLExpr(tombstoneName));
        List<SQLInsertStatement.ValuesClause> valuesList = insertStatement.getValuesList();
        for (SQLInsertStatement.ValuesClause value : valuesList) {
            value.addValue(new SQLIntegerExpr(tombstoneValue));
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
        if (!JdbcSQLUtils.contains(tableName.toString(), tombstoneTableNameList, unTombstoneTableNameList)) {
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        }
        SQLExpr where = mySqlDeleteStatement.getWhere();
        where = SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd,
                SQLUtils.toSQLExpr(tombstoneName + "=" + tombstoneValue, DbType.mysql), false, where);
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
            if (JdbcSQLUtils.contains(table, tombstoneTableNameList, unTombstoneTableNameList)) {
                sqlSelectStatement.accept(new SqlAddTableConditionVisitor(
                        table, tombstoneName + "=" + tombstoneValue, useWhereConditionAsPossible, tableNameEqualCaseInsensitive));
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
        if (!JdbcSQLUtils.contains(tableName.toString(), tombstoneTableNameList, unTombstoneTableNameList)) {
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        }
        List<SQLUpdateSetItem> items = mySqlUpdateStatement.getItems();
        for (SQLUpdateSetItem item : items) {
            SQLExpr column = item.getColumn();
            if (tombstoneName.equals(column.toString())) {
                return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
            }
        }
        SQLUpdateSetItem item = new SQLUpdateSetItem();
        item.setColumn(SQLUtils.toSQLExpr(tombstoneName));
        item.setValue(new SQLIntegerExpr(tombstoneValue));
        items.add(item);
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

}
