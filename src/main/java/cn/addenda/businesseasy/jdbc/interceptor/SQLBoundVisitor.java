package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import java.util.List;

/**
 * @author addenda
 * @since 2023/5/9 18:10
 */
public abstract class SQLBoundVisitor<T extends SQLStatement> extends MySqlASTVisitorAdapter {

    private boolean toLowerCase = true;

    protected String sql;

    protected T sqlStatement;

    protected SQLBoundVisitor(String sql) {
        this.sql = sql;
        this.init();
    }

    protected SQLBoundVisitor(T sqlStatement) {
        this.sqlStatement = sqlStatement;
        this.init();
    }

    private void init() {
        if (sqlStatement == null && sql == null) {
            throw new NullPointerException("SQL 不存在！");
        }
        if (sql == null) {
            this.sql = DruidSQLUtils.toLowerCaseSQL(sqlStatement, toLowerCase);
        }
        if (sqlStatement == null) {
            List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);
            if (stmtList.size() != 1) {
                String msg = String.format("仅支持单条SQL，当前有[%s]条SQL。内容：[%s]。", stmtList.size(), sql);
                throw new IllegalArgumentException(msg);
            }
            this.sqlStatement = (T) stmtList.get(0);
        }
    }

    public abstract T visitAndOutputAst();

    public String visitAndOutputSql() {
        return DruidSQLUtils.toLowerCaseSQL(visitAndOutputAst(), toLowerCase);
    }

    public void visit() {
        visitAndOutputAst();
    }

    public boolean isToLowerCase() {
        return toLowerCase;
    }

    public void setToLowerCase(boolean toLowerCase) {
        this.toLowerCase = toLowerCase;
    }
}
