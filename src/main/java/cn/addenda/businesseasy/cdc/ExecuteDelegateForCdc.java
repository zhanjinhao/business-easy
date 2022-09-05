package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.cdc.sql.SqlUtils;
import cn.addenda.businesseasy.util.BEListUtil;

import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/8/24 22:35
 */
public class ExecuteDelegateForCdc {

    private ExecuteDelegateForCdc() {
    }

    public static <T> T execute(Connection connection, PreparedStatement ps, SqlHolder sqlHolder, StatementFunction<T> sf) throws SQLException {
        String sql = sqlHolder.getParameterizedSql();
        if (SqlUtils.isInsertSql(sql)) {
            return executeInsert(connection, ps, sqlHolder, sf);
        } else if (SqlUtils.isUpdateSql(sql)) {
            return executeUpdate(connection, sqlHolder, sf);
        } else if (SqlUtils.isDeleteSql(sql)) {
            return executeDelete(connection, sqlHolder, sf);
        }
        throw new CdcException("Only support delete, insert, update sql. ");
    }

    /**
     * @param connection
     * @param ps
     * @param sqlHolder
     * @throws SQLException
     */
    public static <T> T executeInsert(Connection connection, PreparedStatement ps, SqlHolder sqlHolder, StatementFunction<T> sf) throws SQLException {
        // 插入的值需要显示在SQL中
        assertPlainValueInUpdateOrInsertPlainValue(sqlHolder.getParameterizedSql());
        T invoke = sf.invoke();
        ResultSet generatedKeys = ps.getGeneratedKeys();

        String keyColumn = sqlHolder.getKeyColumn();
        List<String> executableSqlList = sqlHolder.getExecutableSqlList();
        for (String executableSql : executableSqlList) {
            // 如果SQL中存在主键值，取SQL中的值。
            // 如果SQL中不存在主键值，取自增逐渐的值。
            // 不允许表没有主键。
            // 不支持联合主键。
            BigInteger value = SqlUtils.extractColumnValueFromInsertSql(executableSql, keyColumn, BigInteger.class);
            Long keyValue;
            if (value == null) {
                if (generatedKeys.next()) {
                    keyValue = generatedKeys.getLong(1);
                } else {
                    throw new CdcException("Cannot get key column value from sql or generatedKey, key column: " + keyColumn + ". ");
                }
            } else {
                keyValue = value.longValue();
            }
            String serialNum = UUID.randomUUID().toString();
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(assembleInsertSqlRecordSql(
                        new SqlRecord(executableSql, SqlRecord.TYPE_INSERT, serialNum, String.valueOf(keyValue))));
            }
        }
        return invoke;
    }

    public static <T> T executeDelete(Connection connection, SqlHolder sqlHolder, StatementFunction<T> sf) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            doExecuteDeleteOrUpdate(statement, sqlHolder, SqlRecord.TYPE_DELETE);
        }
        return sf.invoke();
    }

    public static <T> T executeUpdate(Connection connection, SqlHolder sqlHolder, StatementFunction<T> sf) throws SQLException {
        assertPlainValueInUpdateOrInsertPlainValue(sqlHolder.getParameterizedSql());
        assertStableUpdateSql(sqlHolder.getParameterizedSql(), sqlHolder.getKeyColumn());
        T invoke = sf.invoke();
        try (Statement statement = connection.createStatement()) {
            doExecuteDeleteOrUpdate(statement, sqlHolder, SqlRecord.TYPE_UPDATE);
        }
        return invoke;
    }

    private static void doExecuteDeleteOrUpdate(Statement statement, SqlHolder sqlHolder, String type) throws SQLException {
        String tableName = sqlHolder.getTableName();
        String keyColumn = sqlHolder.getKeyColumn();

        List<String> executableSqlList = sqlHolder.getExecutableSqlList();
        for (String executableSql : executableSqlList) {
            // select 获取主键值：
            // 对于 DELETE 语句，select 先于 delete 执行，需要 for update 锁住数据
            // 对于 UPDATE 语句，select 后于 update 执行，不需要锁住数据
            ResultSet resultSet = statement.executeQuery(
                    assembleSelectKeyValueSql(executableSql, tableName, keyColumn, SqlRecord.TYPE_DELETE.equals(type)));
            List<Long> keyValueList = new ArrayList<>();
            while (resultSet.next()) {
                keyValueList.add(resultSet.getLong(keyColumn));
            }
            // 保存至数据库
            String serialNum = UUID.randomUUID().toString();
            List<List<Long>> keyValueListList = BEListUtil.splitList(keyValueList, 50);
            for (List<Long> keyValueSubList : keyValueListList) {
                statement.executeUpdate(assembleInsertSqlRecordSql(new SqlRecord(executableSql, type, serialNum,
                        keyValueSubList.stream().map(Object::toString).collect(Collectors.joining(",")))));
            }
        }
    }

    private static String assembleSelectKeyValueSql(String executableSql, String tableName, String keyColumn, boolean forUpdate) {
        String sql = "select "
                + keyColumn + " "
                + "from "
                + tableName + " "
                + SqlUtils.extractWhereConditionFromUpdateOrDeleteSql(executableSql) + " ";
        return forUpdate ? sql + "for update" : sql;
    }

    private static String assembleInsertSqlRecordSql(SqlRecord sqlEntity) {
        return "insert into t_cdc_sql_record(executable_sql, type, serial_num, records) values("
                + "'" + sqlEntity.getExecutableSql().replace("'", "\\'") + "',"
                + "'" + sqlEntity.getType() + "',"
                + "'" + sqlEntity.getSerialNum() + "',"
                + "'" + sqlEntity.getRecords() + "')";
    }

    private static void assertPlainValueInUpdateOrInsertPlainValue(String sql) {
        if (!SqlUtils.checkPlainValueInUpdateOrInsert(sql)) {
            throw new CdcException("values in update and insert sql must be plain value. such as: 1, 'a', '2022-01-01', 1 + 2, concat('a', 'b'), substring('abc', ?) ...");
        }
    }

    private static void assertStableUpdateSql(String sql, String keyColumn) {
        if (!SqlUtils.checkStableUpdateSql(sql, keyColumn)) {
            throw new CdcException("update sql cannot update column which in where-condition and primary key column. ");
        }
    }

}
