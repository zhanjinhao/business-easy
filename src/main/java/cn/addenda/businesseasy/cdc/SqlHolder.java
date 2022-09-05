package cn.addenda.businesseasy.cdc;

import java.util.List;

/**
 * @author addenda
 * @datetime 2022/8/27 17:31
 */
public class SqlHolder {
    private final String parameterizedSql;
    private final List<String> executableSqlList;
    private final String tableName;
    private final String keyColumn;

    public SqlHolder(String parameterizedSql, List<String> executableSqlList, String tableName, String keyColumn) {
        this.parameterizedSql = parameterizedSql;
        this.executableSqlList = executableSqlList;
        this.tableName = tableName;
        this.keyColumn = keyColumn;
    }

    public String getParameterizedSql() {
        return parameterizedSql;
    }

    public List<String> getExecutableSqlList() {
        return executableSqlList;
    }

    public String getTableName() {
        return tableName;
    }

    public String getKeyColumn() {
        return keyColumn;
    }

}
