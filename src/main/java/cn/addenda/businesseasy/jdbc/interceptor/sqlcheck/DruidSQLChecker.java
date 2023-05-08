package cn.addenda.businesseasy.jdbc.interceptor.sqlcheck;

import cn.addenda.businesseasy.jdbc.interceptor.ExactIdentifierVisitor;
import cn.addenda.businesseasy.jdbc.interceptor.SelectItemStarExistsVisitor;
import cn.addenda.businesseasy.jdbc.interceptor.ViewToTableVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/7 19:58
 */
public class DruidSQLChecker implements SQLChecker {

    @Override
    public boolean exactIdentifier(String sql) {
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        if (sqlStatements.isEmpty()) {
            return true;
        }
        for (SQLStatement statement : sqlStatements) {
            ViewToTableVisitor viewToTableVisitor = new ViewToTableVisitor();
            statement.accept(viewToTableVisitor);
            ExactIdentifierVisitor exactIdentifierVisitor = new ExactIdentifierVisitor();
            statement.accept(exactIdentifierVisitor);
            if (!exactIdentifierVisitor.isExact()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean allColumnExists(String sql) {
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        if (sqlStatements.isEmpty()) {
            return false;
        }
        for (SQLStatement statement : sqlStatements) {
            SelectItemStarExistsVisitor selectItemStarExistsVisitor = new SelectItemStarExistsVisitor();
            statement.accept(selectItemStarExistsVisitor);
            if (selectItemStarExistsVisitor.isExists()) {
                return true;
            }
        }
        return false;
    }

}
