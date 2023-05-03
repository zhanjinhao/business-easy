package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

import java.util.List;
import java.util.function.Function;

/**
 * @author addenda
 * @since 2023/5/2 22:47
 */
public abstract class AbstractDruidSqlRewriter {

    protected String singleRewriteSql(String sql, Function<SQLStatement, String> function) {
        List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);
        StringBuilder stringBuilder = new StringBuilder();
        for (SQLStatement sqlStatement : stmtList) {
            stringBuilder.append(function.apply(sqlStatement)).append("\n");
        }
        return stringBuilder.toString().trim();
    }

}
