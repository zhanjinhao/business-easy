package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

/**
 * @author addenda
 * @since 2023/4/28 10:05
 */
public class SqlAddViewConditionVisitor extends SqlAddTableConditionVisitor {

    public SqlAddViewConditionVisitor(String condition) {
        super(condition);
    }

    public SqlAddViewConditionVisitor(String tableName, String condition) {
        super(tableName, condition);
    }

    public SqlAddViewConditionVisitor(String tableName, String condition, boolean useWhereConditionAsPossible) {
        super(tableName, condition, useWhereConditionAsPossible);
    }

    public SqlAddViewConditionVisitor(
            String tableName, String condition, boolean useWhereConditionAsPossible, boolean tableNameEqualCaseInsensitive) {
        super(tableName, condition, useWhereConditionAsPossible, tableNameEqualCaseInsensitive);
    }

    @Override
    protected String getTableName(String tableName, String alias) {
        return alias == null ? tableName : alias;
    }

}