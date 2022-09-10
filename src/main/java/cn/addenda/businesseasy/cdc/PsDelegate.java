package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.asynctask.BinaryResult;
import cn.addenda.businesseasy.cdc.sql.SqlUtils;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;

/**
 * @author addenda
 * @datetime 2022/8/24 22:35
 */
public class PsDelegate {

    private PsDelegate() {
    }

    public static <T> T execute(Connection connection, PreparedStatement ps, CdcContext cdcContext, PsInvocation<T> sf) throws SQLException {
        String sql = cdcContext.getParameterizedSql();
        if (SqlUtils.isInsertSql(sql)) {
            return executeInsert(connection, ps, cdcContext, sf);
        } else if (SqlUtils.isUpdateSql(sql)) {
            return executeUpdate(connection, cdcContext, sf);
        } else if (SqlUtils.isDeleteSql(sql)) {
            return executeDelete(connection, cdcContext, sf);
        }
        throw new CdcException("Only support delete, insert, update sql. ");
    }

    /**
     * @param connection
     * @param ps
     * @param cdcContext
     * @throws SQLException
     */
    public static <T> T executeInsert(Connection connection, PreparedStatement ps, CdcContext cdcContext, PsInvocation<T> sf) throws SQLException {
        T invoke = sf.invoke();
        List<String> executableSqlList = cdcContext.getExecutableSqlList();
        String keyColumn = cdcContext.getKeyColumn();


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
            if (cdcContext.checkTableMode(TableConfig.CM_STATEMENT)) {
                statementCdcSqlList.add(assembleCdcRecordSql(cdcContext.getTableName(), TableConfig.CM_STATEMENT, executableSql));
            }
            // 对于ROW模式，需要记录下来具体插入的值。
            if (cdcContext.checkTableMode(TableConfig.CM_ROW)) {
                List<String> columnList = SqlUtils.extractNonLiteralColumnFromUpdateOrDeleteSql(executableSql);
                if (!columnList.isEmpty()) {
                    Map<String, Object> columnValueMap = queryColumnValueMap(connection, cdcContext, keyValue.getFirstResult(), columnList);
                    executableSql = SqlUtils.UpdateOrInsertUpdateColumnValue(executableSql, columnValueMap);
                }
                rowCdcSqlList.add(assembleCdcRecordSql(cdcContext.getTableName(), TableConfig.CM_ROW, executableSql));
            }
        }
        executeCdcSql(connection, statementCdcSqlList);
        executeCdcSql(connection, rowCdcSqlList);

        return invoke;
    }

    private static Map<String, Object> queryColumnValueMap(Connection connection, CdcContext cdcContext, Long key, List<String> columnList) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            Map<String, Object> map = new HashMap<>();
            String sql = "select "
                    + String.join(",", columnList) + " "
                    + "from " + cdcContext.getTableName() + " "
                    + "where " + cdcContext.getKeyColumn() + " "
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

    public static <T> T executeDelete(Connection connection, CdcContext cdcContext, PsInvocation<T> sf) throws SQLException {
        String tableName = cdcContext.getTableName();
        String keyColumn = cdcContext.getKeyColumn();
        List<String> statementCdcSqlList = new ArrayList<>();
        List<String> rowCdcSqlList = new ArrayList<>();
        List<String> executableSqlList = cdcContext.getExecutableSqlList();
        for (String executableSql : executableSqlList) {
            // 对于Statement模式来说，记录下来SQL就行了
            if (cdcContext.checkTableMode(TableConfig.CM_STATEMENT)) {
                statementCdcSqlList.add(assembleCdcRecordSql(cdcContext.getTableName(), TableConfig.CM_STATEMENT, executableSql));
            }
            // 对于ROW模式，需要记录下来具体删除的行。
            if (cdcContext.checkTableMode(TableConfig.CM_ROW)) {
                try (Statement statement = connection.createStatement()) {
                    // select 获取主键值：对于 DELETE 语句，select 先于 delete 执行，需要 for update 锁住数据
                    ResultSet resultSet = statement.executeQuery(
                            assembleSelectKeyValueSql(executableSql, tableName, keyColumn, true));
                    while (resultSet.next()) {
                        executableSql = "delete from " + tableName + " where " + keyColumn + "=" + resultSet.getLong(keyColumn);
                        rowCdcSqlList.add(assembleCdcRecordSql(cdcContext.getTableName(), TableConfig.CM_ROW, executableSql));
                    }
                }
            }
        }
        executeCdcSql(connection, statementCdcSqlList);
        executeCdcSql(connection, rowCdcSqlList);

        return sf.invoke();
    }

    public static <T> T executeUpdate(Connection connection, CdcContext cdcContext, PsInvocation<T> sf) throws SQLException {
        assertStableUpdateSql(cdcContext.getParameterizedSql(), cdcContext.getKeyColumn());

        T invoke = sf.invoke();
        String tableName = cdcContext.getTableName();
        String keyColumn = cdcContext.getKeyColumn();
        List<String> statementCdcSqlList = new ArrayList<>();
        List<String> rowCdcSqlList = new ArrayList<>();
        List<String> executableSqlList = cdcContext.getExecutableSqlList();
        for (String executableSql : executableSqlList) {
            // 对于Statement模式来说，记录下来SQL就行了
            if (cdcContext.checkTableMode(TableConfig.CM_STATEMENT)) {
                statementCdcSqlList.add(assembleCdcRecordSql(cdcContext.getTableName(), TableConfig.CM_STATEMENT, executableSql));
            }
            // 对于ROW模式，需要记录下来具体更新的行。
            if (cdcContext.checkTableMode(TableConfig.CM_ROW)) {
                try (Statement statement = connection.createStatement()) {
                    // select 获取主键值：对于 UPDATE 语句，select 先于 update 执行，不需要 for update 锁住数据
                    ResultSet resultSet = statement.executeQuery(
                            assembleSelectKeyValueSql(executableSql, tableName, keyColumn, false));
                    List<String> columnList = SqlUtils.extractNonLiteralColumnFromUpdateOrDeleteSql(executableSql);
                    while (resultSet.next()) {
                        long keyValue = resultSet.getLong(keyColumn);
                        String rowCdcSql = SqlUtils.replaceDmlWhereSeg(executableSql, "where " + keyColumn + " = " + keyValue);
                        if (!columnList.isEmpty()) {
                            Map<String, Object> columnValueMap = queryColumnValueMap(connection, cdcContext, keyValue, columnList);
                            rowCdcSql = SqlUtils.UpdateOrInsertUpdateColumnValue(rowCdcSql, columnValueMap);
                        }
                        rowCdcSqlList.add(assembleCdcRecordSql(cdcContext.getTableName(), TableConfig.CM_ROW, rowCdcSql));
                    }
                }
            }
        }

        executeCdcSql(connection, statementCdcSqlList);
        executeCdcSql(connection, rowCdcSqlList);
        return invoke;
    }

    private static String assembleSelectKeyValueSql(String executableSql, String tableName, String keyColumn, boolean forUpdate) {
        String sql = "select "
                + keyColumn + " "
                + "from "
                + tableName + " "
                + SqlUtils.extractWhereConditionFromUpdateOrDeleteSql(executableSql) + " ";
        return forUpdate ? sql + "for update" : sql;
    }

    private static String assembleCdcRecordSql(String tableName, String cdcMode, String executableSql) {
        return "insert into " + tableName + "_cdc_" + cdcMode + "(executable_sql) values("
                + "'" + executableSql.replace("'", "\\'") + "')";
    }

    private static void assertStableUpdateSql(String sql, String keyColumn) {
        if (!SqlUtils.checkStableUpdateSql(sql, keyColumn)) {
            throw new CdcException("update sql cannot update column which in where-condition and primary key column. ");
        }
    }

    private static void executeCdcSql(Connection connection, List<String> cdcSqlList) throws SQLException {
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

}
