package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.AbstractDruidSqlRewriter;
import cn.addenda.businesseasy.jdbc.interceptor.DruidSQLUtils;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @since 2023/5/2 19:35
 */
@Slf4j
public class DruidBaseEntityRewriter extends AbstractDruidSqlRewriter implements BaseEntityRewriter {

    private BaseEntitySource baseEntitySource;

    /**
     * 需要基础字段的表
     */
    private List<String> baseEntityTableNameList;

    /**
     * 不需要基础字段的表
     */
    private List<String> unBaseEntityTableNameList = new ArrayList<>(Arrays.asList("dual"));

    private static final List<String> INSERT_COLUMN_NAME_LIST;
    private static final List<String> INSERT_FIELD_NAME_LIST;
    private static final List<String> UPDATE_COLUMN_NAME_LIST;
    private static final List<String> UPDATE_FIELD_NAME_LIST;

    static {
        INSERT_COLUMN_NAME_LIST = BaseEntity.getAllColumnNameList();
        INSERT_FIELD_NAME_LIST = BaseEntity.getAllFieldNameList();
        UPDATE_COLUMN_NAME_LIST = BaseEntity.getUpdateColumnNameList();
        UPDATE_FIELD_NAME_LIST = BaseEntity.getUpdateFieldNameList();
    }

    public DruidBaseEntityRewriter(List<String> baseEntityTableNameList, BaseEntitySource baseEntitySource) {
        this.baseEntitySource = baseEntitySource;
        if (baseEntityTableNameList != null) {
            this.baseEntityTableNameList = new ArrayList<>();
            this.baseEntityTableNameList.addAll(baseEntityTableNameList);
        } else {
            log.warn("未声明需填充的基础字段的表集合，所有的表都会进行基础字段填充改写！");
        }
    }

    @Override
    public String rewriteInsertSql(String sql) {
        return singleRewriteSql(sql, this::doRewriteInsertSql);
    }

    private String doRewriteInsertSql(SQLStatement sqlStatement) {
        MySqlInsertStatement mySqlInsertStatement = (MySqlInsertStatement) sqlStatement;
        SQLName tableName = mySqlInsertStatement.getTableName();
        if (!JdbcSQLUtils.contains(tableName.toString(), baseEntityTableNameList, unBaseEntityTableNameList)) {
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        }
        byte[] injects = new byte[INSERT_COLUMN_NAME_LIST.size()];
        List<SQLExpr> columnList = mySqlInsertStatement.getColumns();
        List<String> columnNameList = columnList.stream().map(i -> i.toString().toLowerCase()).collect(Collectors.toList());
        for (int i = 0; i < INSERT_COLUMN_NAME_LIST.size(); i++) {
            String column = INSERT_COLUMN_NAME_LIST.get(i);
            if (columnNameList.contains(column.toLowerCase())) {
                injects[i] = 1;
            } else {
                injects[i] = 0;
                columnList.add(SQLUtils.toSQLExpr(column));
            }
        }

        List<SQLInsertStatement.ValuesClause> valuesList = mySqlInsertStatement.getValuesList();
        for (SQLInsertStatement.ValuesClause values : valuesList) {
            for (int i = 0; i < INSERT_FIELD_NAME_LIST.size(); i++) {
                if (injects[i] == 0) {
                    String field = INSERT_FIELD_NAME_LIST.get(i);
                    Object o = baseEntitySource.get(field);
                    values.addValue(DruidSQLUtils.objectToSQLExpr(o));
                }
            }
        }

        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    @Override
    public String rewriteDeleteSql(String sql) {
        return sql;
    }

    @Override
    public String rewriteSelectSql(String sql, String masterView) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);
        StringBuilder stringBuilder = new StringBuilder();
        for (SQLStatement sqlStatement : stmtList) {
            stringBuilder.append(doRewriteSelectSql(sqlStatement, masterView)).append("\n");
        }
        return stringBuilder.toString().trim();
    }

    private String doRewriteSelectSql(SQLStatement sqlStatement, String masterView) {
        SQLSelectStatement sqlSelectStatement = (SQLSelectStatement) sqlStatement;
        SchemaStatVisitor schemaStatVisitor = new SchemaStatVisitor();
        sqlSelectStatement.accept(schemaStatVisitor);
        Map<TableStat.Name, TableStat> tables = schemaStatVisitor.getTables();
        Set<String> collect = tables.keySet().stream().map(TableStat.Name::getName).collect(Collectors.toSet());
        for (String table : collect) {
            if (JdbcSQLUtils.contains(table, baseEntityTableNameList, unBaseEntityTableNameList)) {
                sqlSelectStatement = new DruidSelectAddBaseEntityVisitor(
                        baseEntityTableNameList, unBaseEntityTableNameList, masterView, sqlSelectStatement).visit();
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
        if (!JdbcSQLUtils.contains(tableName.toString(), baseEntityTableNameList, unBaseEntityTableNameList)) {
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        }

        List<SQLUpdateSetItem> items = mySqlUpdateStatement.getItems();
        List<String> columnList = items.stream().map(i -> i.getColumn().toString()).collect(Collectors.toList());

        for (int i = 0; i < UPDATE_COLUMN_NAME_LIST.size(); i++) {
            String column = UPDATE_COLUMN_NAME_LIST.get(i);
            String field = UPDATE_FIELD_NAME_LIST.get(i);
            if (!columnList.contains(column)) {
                SQLUpdateSetItem item = new SQLUpdateSetItem();
                item.setColumn(SQLUtils.toSQLExpr(column));
                item.setValue(DruidSQLUtils.objectToSQLExpr(baseEntitySource.get(field)));
                items.add(item);
            }
        }

        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }
}
