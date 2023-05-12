package cn.addenda.businesseasy.jdbc.interceptor;

import java.util.List;

/**
 * @author addenda
 * @since 2023/4/28 10:05
 */
public class ViewAddJoinConditionVisitor extends TableAddJoinConditionVisitor {

    public ViewAddJoinConditionVisitor(String condition) {
        super(condition);
    }

    public ViewAddJoinConditionVisitor(String tableName, String condition) {
        super(tableName, condition);
    }

    public ViewAddJoinConditionVisitor(String tableName, String condition, boolean useSubQuery) {
        super(tableName, condition, useSubQuery);
    }

    public ViewAddJoinConditionVisitor(List<String> included, List<String> notIncluded, String condition, boolean useSubQuery, boolean reWriteCommaToJoin) {
        super(included, notIncluded, condition, useSubQuery, reWriteCommaToJoin);
    }

    @Override
    protected String determineTableName(String tableName, String alias) {
        return alias == null ? tableName : alias;
    }

}
