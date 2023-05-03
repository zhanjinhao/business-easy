package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

import cn.addenda.businesseasy.jdbc.interceptor.DruidSQLUtils;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

import java.util.List;

/**
 * @author addenda
 * @datetime 2023/4/30 16:56
 */
public class DruidDynamicConditionAssembler implements DynamicConditionAssembler {

    private boolean useWhereConditionAsPossible = false;
    private boolean tableNameEqualCaseInsensitive = true;

    public DruidDynamicConditionAssembler() {
    }

    public DruidDynamicConditionAssembler(boolean useWhereConditionAsPossible, boolean tableNameEqualCaseInsensitive) {
        this.useWhereConditionAsPossible = useWhereConditionAsPossible;
        this.tableNameEqualCaseInsensitive = tableNameEqualCaseInsensitive;
    }

    @Override
    public String tableAddCondition(
            String sql, String tableName, String condition) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);
        StringBuilder stringBuilder = new StringBuilder();
        for (SQLStatement sqlStatement : stmtList) {
            sqlStatement.accept(new SqlAddTableConditionVisitor(
                    tableName, condition, useWhereConditionAsPossible, tableNameEqualCaseInsensitive));
            stringBuilder.append(DruidSQLUtils.toLowerCaseSQL(sqlStatement)).append("\n");
        }
        return stringBuilder.toString().trim();
    }

    @Override
    public String viewAddCondition(
            String sql, String tableName, String condition) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);
        StringBuilder stringBuilder = new StringBuilder();
        for (SQLStatement sqlStatement : stmtList) {
            sqlStatement.accept(new SqlAddViewConditionVisitor(
                    tableName, condition, useWhereConditionAsPossible, tableNameEqualCaseInsensitive));
            stringBuilder.append(DruidSQLUtils.toLowerCaseSQL(sqlStatement)).append("\n");
        }
        return stringBuilder.toString();
    }

}
