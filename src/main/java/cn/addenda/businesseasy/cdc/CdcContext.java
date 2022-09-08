package cn.addenda.businesseasy.cdc;

import java.util.List;

/**
 * @author addenda
 * @datetime 2022/8/27 17:31
 */
public class CdcContext {
    private final String parameterizedSql;
    private final List<String> executableSqlList;
    private final TableConfig tableConfig;

    public CdcContext(String parameterizedSql, List<String> executableSqlList, TableConfig tableConfig) {
        this.parameterizedSql = parameterizedSql;
        this.executableSqlList = executableSqlList;
        this.tableConfig = tableConfig;
    }

    public String getParameterizedSql() {
        return parameterizedSql;
    }

    public List<String> getExecutableSqlList() {
        return executableSqlList;
    }

    public String getTableName() {
        return tableConfig.getTableName();
    }

    public String getKeyColumn() {
        return tableConfig.getKeyColumn();
    }

    public boolean checkTableMode(String mode) {
        if (tableConfig == null) {
            return false;
        }
        List<String> cdcModeList = tableConfig.getCdcModeList();
        return cdcModeList.contains(mode);
    }

}
