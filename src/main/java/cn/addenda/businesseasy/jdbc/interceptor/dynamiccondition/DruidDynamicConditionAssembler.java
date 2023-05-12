package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

import cn.addenda.businesseasy.jdbc.interceptor.DruidSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.TableAddJoinConditionVisitor;
import cn.addenda.businesseasy.jdbc.interceptor.ViewAddJoinConditionVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import java.util.List;

/**
 * todo: <br/>
 * - 表增加条件； <br/>
 * - 视图增加条件； <br/>
 * - insert增加item； <br/>
 * - update增加item； <br/>
 * - where增加条件；<br/>
 *
 * @author addenda
 * @since 2023/4/30 16:56
 */
public class DruidDynamicConditionAssembler implements DynamicConditionAssembler {

    private boolean useConditionAsPossible = false;
    private boolean tableNameEqualCaseInsensitive = true;

    public DruidDynamicConditionAssembler() {
    }

    public DruidDynamicConditionAssembler(boolean useConditionAsPossible, boolean tableNameEqualCaseInsensitive) {
        this.useConditionAsPossible = useConditionAsPossible;
        this.tableNameEqualCaseInsensitive = tableNameEqualCaseInsensitive;
    }

    @Override
    public String tableAddCondition(
        String sql, String tableName, String condition) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);
        StringBuilder stringBuilder = new StringBuilder();
        for (SQLStatement sqlStatement : stmtList) {
            sqlStatement.accept(new TableAddJoinConditionVisitor(tableName, condition, useConditionAsPossible));
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
            sqlStatement.accept(new ViewAddJoinConditionVisitor(tableName, condition, useConditionAsPossible));
            stringBuilder.append(DruidSQLUtils.toLowerCaseSQL(sqlStatement)).append("\n");
        }
        return stringBuilder.toString();
    }

}
