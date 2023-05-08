package cn.addenda.businesseasy.jdbc.interceptor.sqlcheck;

import cn.addenda.businesseasy.jdbc.interceptor.ConnectionPrepareStatementInterceptor;
import cn.addenda.businesseasy.jdbc.interceptor.DruidSQLUtils;

/**
 * - 返回值为*或tableName.*检测 <br/>
 * - 精确标识符检测 <br/>
 *
 * @author addenda
 * @since 2023/5/7 15:53
 */
public class SQLCheckInterceptor extends ConnectionPrepareStatementInterceptor {

    private final SQLChecker sqlChecker;
    private final boolean checkAllColumn;
    private final boolean removeEnter;
    private final boolean checkExactIdentifier;

    public SQLCheckInterceptor() {
        this.sqlChecker = new DruidSQLChecker();
        this.checkAllColumn = true;
        this.checkExactIdentifier = true;
        this.removeEnter = true;
    }

    public SQLCheckInterceptor(SQLChecker sqlChecker) {
        this.sqlChecker = sqlChecker;
        this.checkAllColumn = true;
        this.checkExactIdentifier = true;
        this.removeEnter = true;
    }

    public SQLCheckInterceptor(boolean checkAllColumn, boolean checkExactIdentifier, boolean removeEnter, SQLChecker sqlChecker) {
        this.sqlChecker = sqlChecker;
        this.checkAllColumn = checkAllColumn;
        this.checkExactIdentifier = checkExactIdentifier;
        this.removeEnter = removeEnter;
    }

    @Override
    protected String process(String sql) {
        if (checkAllColumn && SQLCheckContext.getCheckAllColumn() && sqlChecker.allColumnExists(sql)) {
            String msg = String.format("SQL: [%s], 返回字段包含了*或tableName.*语法！", removeEnter(sql));
            throw new SQLCheckException(msg);
        }
        if (checkExactIdentifier && SQLCheckContext.getCheckExactIdentifier() && !sqlChecker.exactIdentifier((sql))) {
            String msg = String.format("SQL: [%s], 存在不精确的字段！", removeEnter(sql));
            throw new SQLCheckException(msg);
        }

        return sql;
    }

    private String removeEnter(String sql) {
        if (removeEnter) {
            return DruidSQLUtils.removeEnter(sql);
        }
        return sql;
    }

}
