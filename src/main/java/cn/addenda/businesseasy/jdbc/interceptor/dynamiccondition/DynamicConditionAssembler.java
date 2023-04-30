package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

/**
 * @author addenda
 * @datetime 2023/4/30 16:56
 */
public interface DynamicConditionAssembler {

    String tableAddCondition(String sql, String tableName, String condition);

    String viewAddCondition(String sql, String tableName, String condition);

}
