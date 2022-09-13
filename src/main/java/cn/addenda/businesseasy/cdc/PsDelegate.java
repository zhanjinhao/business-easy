package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.asynctask.BinaryResult;
import cn.addenda.businesseasy.cdc.format.DataFormatterRegistry;
import cn.addenda.businesseasy.cdc.sql.SqlUtils;
import cn.addenda.ro.grammar.ast.expression.CurdType;
import cn.addenda.ro.grammar.lexical.token.Token;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/8/24 22:35
 */
public class PsDelegate {

    private final CdcDataSource cdcDataSource;

    private final CdcConnection cdcConnection;

    private final PreparedStatement ps;

    private final TableConfig tableConfig;

    private final String parameterizedSql;

    private final String tableName;

    private final String keyColumn;

    private final DataFormatterRegistry dataFormatterRegistry;

    private final CurdType curdType;

    public PsDelegate(CdcDataSource cdcDataSource, CdcConnection cdcConnection, PreparedStatement ps, TableConfig tableConfig, String parameterizedSql) {
        this.cdcDataSource = cdcDataSource;
        this.cdcConnection = cdcConnection;
        this.ps = ps;
        this.tableConfig = tableConfig;
        this.parameterizedSql = parameterizedSql;
        this.tableName = tableConfig.getTableName();
        this.keyColumn = tableConfig.getKeyColumn();
        this.dataFormatterRegistry = cdcDataSource.getDataFormatterRegistry();
        if (SqlUtils.isInsertSql(parameterizedSql)) {
            curdType = CurdType.INSERT;
        } else if (SqlUtils.isUpdateSql(parameterizedSql)) {
            curdType = CurdType.UPDATE;
        } else if (SqlUtils.isDeleteSql(parameterizedSql)) {
            curdType = CurdType.DELETE;
        } else {
            throw new CdcException("Only support delete, insert, update sql. ");
        }
    }

    public <T> T execute(List<String> executableSqlList, PsInvocation<T> sf) throws SQLException {
        if (CurdType.INSERT.equals(curdType)) {
            return executeInsert(executableSqlList, sf);
        } else if (CurdType.UPDATE.equals(curdType)) {
            return executeUpdate(executableSqlList, sf);
        } else if (CurdType.DELETE.equals(curdType)) {
            return executeDelete(executableSqlList, sf);
        }
        throw new CdcException("Only support delete, insert, update sql. ");
    }

    public <T> T executeInsert(List<String> executableSqlList, PsInvocation<T> sf) throws SQLException {
        T invoke = sf.invoke();

        // -----------
        //  处理主键值
        // -----------
        ResultSet generatedKeys = ps.getGeneratedKeys();
        List<BinaryResult<Long, Byte>> keyValueList = new ArrayList<>();
        for (String executableSql : executableSqlList) {
            // 如果SQL中存在主键值，取SQL中的值。
            // 如果SQL中不存在主键值，取自增逐渐的值。
            // 不允许表没有主键。不支持联合主键。
            BigInteger value = SqlUtils.extractColumnValueFromInsertSql(executableSql, keyColumn, BigInteger.class);
            long keyValue;
            if (value == null) {
                if (generatedKeys.next()) {
                    keyValue = generatedKeys.getLong(1);
                    keyValueList.add(new BinaryResult<>(keyValue, (byte) 0));
                } else {
                    throw new CdcException("Cannot get key column value from sql or generatedKey, key column: " + keyColumn + ". ");
                }
            } else {
                keyValue = value.longValue();
                keyValueList.add(new BinaryResult<>(keyValue, (byte) 1));
            }
        }


        // -----------------
        //  生成CDC SQL并记录
        // -----------------
        List<String> statementCdcSqlList = new ArrayList<>();
        List<String> rowCdcSqlList = new ArrayList<>();
        for (int i = 0; i < executableSqlList.size(); i++) {
            String executableSql = executableSqlList.get(i);
            BinaryResult<Long, Byte> keyValue = keyValueList.get(i);
            if (keyValue.getSecondResult() == 0) {
                executableSql = SqlUtils.insertInjectColumnValue(executableSql, keyColumn, keyValue.getFirstResult());
            }
            // 对于Statement模式来说，记录下来SQL就行了
            if (checkTableMode(TableConfig.CM_STATEMENT)) {
                statementCdcSqlList.add(assembleCdcRecordSql(tableName, TableConfig.CM_STATEMENT, executableSql));
            }
            // 对于ROW模式，需要记录下来具体插入的值。
            if (checkTableMode(TableConfig.CM_ROW)) {
                List<String> columnList = SqlUtils.extractNonLiteralColumnFromUpdateOrDeleteSql(executableSql);
                if (!columnList.isEmpty()) {
                    Map<String, Object> columnValueMap = queryColumnValueMap(keyValue.getFirstResult(), columnList);
                    Map<String, Token> columnTokenMap = columnValueMap.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> dataFormatterRegistry.parse(e.getValue())));
                    executableSql = SqlUtils.UpdateOrInsertUpdateColumnValue(executableSql, columnTokenMap);
                }
                rowCdcSqlList.add(assembleCdcRecordSql(tableName, TableConfig.CM_ROW, executableSql));
            }
        }

        executeCdcSql(statementCdcSqlList);
        executeCdcSql(rowCdcSqlList);
        return invoke;
    }

    private Map<String, Object> queryColumnValueMap(Long key, List<String> columnList) throws SQLException {
        try (Statement statement = cdcConnection.getDelegate().createStatement()) {
            Map<String, Object> map = new HashMap<>();
            String sql = "select "
                    + String.join(",", columnList) + " "
                    + "from " + tableName + " "
                    + "where " + tableName + " "
                    + "=" + key;
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                for (String column : columnList) {
                    map.put(column, resultSet.getObject(column));
                }
            } else {
                throw new CdcException("Cannot get key column value from resultSet. ");
            }
            return map;
        }
    }

    public <T> T executeDelete(List<String> executableSqlList, PsInvocation<T> sf) throws SQLException {
        List<String> statementCdcSqlList = new ArrayList<>();
        List<String> rowCdcSqlList = new ArrayList<>();
        for (String executableSql : executableSqlList) {
            // 对于Statement模式来说，记录下来SQL就行了
            if (checkTableMode(TableConfig.CM_STATEMENT)) {
                statementCdcSqlList.add(assembleCdcRecordSql(tableName, TableConfig.CM_STATEMENT, executableSql));
            }
            // 对于ROW模式，需要记录下来具体删除的行。
            if (checkTableMode(TableConfig.CM_ROW)) {
                try (Statement statement = cdcConnection.getDelegate().createStatement()) {
                    // select 获取主键值。
                    ResultSet resultSet = statement.executeQuery(
                            assembleSelectKeyValueSql(executableSql, tableName, keyColumn));
                    while (resultSet.next()) {
                        executableSql = "delete from " + tableName + " where " + keyColumn + "=" + resultSet.getLong(keyColumn);
                        rowCdcSqlList.add(assembleCdcRecordSql(tableName, TableConfig.CM_ROW, executableSql));
                    }
                }
            }
        }

        executeCdcSql(statementCdcSqlList);
        executeCdcSql(rowCdcSqlList);
        return sf.invoke();
    }

    public <T> T executeUpdate(List<String> executableSqlList, PsInvocation<T> sf) throws SQLException {
        assertStableUpdateSql(parameterizedSql, keyColumn);
        List<String> statementCdcSqlList = new ArrayList<>();
        List<String> rowCdcSqlList = new ArrayList<>();
        for (String executableSql : executableSqlList) {
            // 对于Statement模式来说，记录下来SQL就行了
            if (checkTableMode(TableConfig.CM_STATEMENT)) {
                statementCdcSqlList.add(assembleCdcRecordSql(tableName, TableConfig.CM_STATEMENT, executableSql));
            }
            // 对于ROW模式，需要记录下来具体更新的行。
            if (checkTableMode(TableConfig.CM_ROW)) {
                try (Statement statement = cdcConnection.getDelegate().createStatement()) {
                    // select 获取主键值。
                    ResultSet resultSet = statement.executeQuery(
                            assembleSelectKeyValueSql(executableSql, tableName, keyColumn));
                    List<String> columnList = SqlUtils.extractNonLiteralColumnFromUpdateOrDeleteSql(executableSql);
                    while (resultSet.next()) {
                        long keyValue = resultSet.getLong(keyColumn);
                        String rowCdcSql = SqlUtils.replaceDmlWhereSeg(executableSql, "where " + keyColumn + " = " + keyValue);
                        if (!columnList.isEmpty()) {
                            Map<String, Object> columnValueMap = queryColumnValueMap(keyValue, columnList);
                            Map<String, Token> columnTokenMap = columnValueMap.entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, e -> cdcDataSource.getDataFormatterRegistry().parse(e.getValue())));
                            rowCdcSql = SqlUtils.UpdateOrInsertUpdateColumnValue(rowCdcSql, columnTokenMap);
                        }
                        rowCdcSqlList.add(assembleCdcRecordSql(tableName, TableConfig.CM_ROW, rowCdcSql));
                    }
                }
            }
        }

        executeCdcSql(statementCdcSqlList);
        executeCdcSql(rowCdcSqlList);
        return sf.invoke();
    }

    private String assembleSelectKeyValueSql(String executableSql, String tableName, String keyColumn) {
        return "select "
                + keyColumn + " "
                + "from "
                + tableName + " "
                + SqlUtils.extractWhereConditionFromUpdateOrDeleteSql(executableSql) + " "
                + "for update";
    }

    private String assembleCdcRecordSql(String tableName, String cdcMode, String executableSql) {
        return "insert into " + tableName + "_cdc_" + cdcMode + "(executable_sql) values("
                + "'" + executableSql.replace("'", "\\'") + "')";
    }

    private void assertStableUpdateSql(String sql, String keyColumn) {
        if (!SqlUtils.checkStableUpdateSql(sql, keyColumn)) {
            throw new CdcException("update sql cannot update column which in where-condition and primary key column. ");
        }
    }

    private void executeCdcSql(List<String> cdcSqlList) throws SQLException {
        Connection connection = cdcConnection.getDelegate();
        if (cdcSqlList == null || cdcSqlList.isEmpty()) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            for (int i = 0; i < cdcSqlList.size(); i++) {
                statement.addBatch(cdcSqlList.get(i));
                if (i != 0 && i % 100 == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
        }
    }

    public boolean checkTableMode(String mode) {
        if (tableConfig == null) {
            return false;
        }
        List<String> cdcModeList = tableConfig.getCdcModeList();
        return cdcModeList.contains(mode);
    }

}
