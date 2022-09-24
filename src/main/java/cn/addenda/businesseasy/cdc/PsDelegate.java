package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.asynctask.BinaryResult;
import cn.addenda.businesseasy.cdc.format.DataFormatterRegistry;
import cn.addenda.businesseasy.cdc.sql.SqlUtils;
import cn.addenda.businesseasy.util.BEListUtil;
import cn.addenda.ro.grammar.ast.expression.CurdType;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/8/24 22:35
 */
public class PsDelegate {

    public static final int IN_SIZE = 2;
    public static final int EXECUTE_INSERT_SQL_BATCH = 10;
    public static final int EXECUTE_INSERT_STATEMENT_BATCH = 20;

    private final CdcConnection cdcConnection;

    private final PreparedStatement ps;

    private final TableConfig tableConfig;

    private final String parameterizedSql;

    private final String tableName;

    private final String keyColumn;

    private final DataFormatterRegistry dataFormatterRegistry;

    private final CurdType curdType;

    public PsDelegate(CdcConnection cdcConnection, PreparedStatement ps, TableConfig tableConfig, String parameterizedSql) {
        this.cdcConnection = cdcConnection;
        this.ps = ps;
        this.tableConfig = tableConfig;
        this.parameterizedSql = parameterizedSql;
        this.tableName = tableConfig.getTableName();
        this.keyColumn = tableConfig.getKeyColumn();
        this.dataFormatterRegistry = cdcConnection.getCdcDataSource().getDataFormatterRegistry();
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

    public <T> T execute(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        if (CurdType.INSERT.equals(curdType)) {
            return executeInsert(executableSqlList, pi);
        } else if (CurdType.UPDATE.equals(curdType)) {
            return executeUpdate(executableSqlList, pi);
        } else if (CurdType.DELETE.equals(curdType)) {
            return executeDelete(executableSqlList, pi);
        }
        throw new CdcException("Only support delete, insert, update sql. ");
    }

    public <T> T executeInsert(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        T invoke = pi.invoke();
        BinaryResult<List<String>, List<Long>> sqlWithKeyValueBr = fillKeyValueToInsertSql(executableSqlList);
        executableSqlList = sqlWithKeyValueBr.getFirstResult();
        List<Long> keyValueList = sqlWithKeyValueBr.getSecondResult();

        // -------------------------------------
        //  对于Statement模式来说，记录下来SQL就行了
        // -------------------------------------
        if (checkTableMode(TableConfig.CM_STATEMENT)) {
            List<String> statementCdcSqlList = new ArrayList<>(executableSqlList);
            executeCdcSql(TableConfig.CM_STATEMENT, statementCdcSqlList);
        }

        // ----------------------------------
        //  对于ROW模式，需要记录下来具体插入的值。
        // ----------------------------------
        if (checkTableMode(TableConfig.CM_ROW)) {
            List<String> rowCdcSqlList = new ArrayList<>();
            List<String> columnList = SqlUtils.extractNonLiteralColumnFromUpdateOrInsertSql(parameterizedSql);
            if (!columnList.isEmpty()) {
                try (Statement statement = cdcConnection.getDelegate().createStatement()) {
                    Map<Long, Map<String, Token>> keyColumnTokenMap = queryKeyColumnTokenMap(statement, keyValueList, columnList);
                    for (int i = 0; i < keyValueList.size(); i++) {
                        Long keyValue = keyValueList.get(i);
                        Map<String, Token> columnTokenMap = keyColumnTokenMap.get(keyValue);
                        rowCdcSqlList.add(SqlUtils.updateOrInsertUpdateColumnValue(executableSqlList.get(i), columnTokenMap));
                    }
                }
            } else {
                rowCdcSqlList.addAll(executableSqlList);
            }
            executeCdcSql(TableConfig.CM_ROW, rowCdcSqlList);
        }

        return invoke;
    }

    /**
     * 处理SQL语句 和 主键值
     *
     * @param executableSqlList
     * @return firstValue时填充了主键值的sql；secondValue主键集合。
     * @throws SQLException
     */
    private BinaryResult<List<String>, List<Long>> fillKeyValueToInsertSql(List<String> executableSqlList) throws SQLException {
        ResultSet generatedKeys = ps.getGeneratedKeys();
        List<Long> keyValueList = new ArrayList<>();
        List<String> executableSqlWithKeyValueList = new ArrayList<>();
        for (String executableSql : executableSqlList) {
            // 如果SQL中存在主键值，取SQL中的值。
            // 如果SQL中不存在主键值，取自增主键的值。
            // 不允许表没有主键。不支持联合主键。
            BigInteger value = SqlUtils.extractColumnValueFromInsertSql(executableSql, keyColumn, BigInteger.class);
            long keyValue;
            if (value == null) {
                if (generatedKeys.next()) {
                    keyValue = generatedKeys.getLong(1);
                    keyValueList.add(keyValue);
                    executableSqlWithKeyValueList.add(SqlUtils.insertInjectColumnValue(executableSql, keyColumn, new Token(TokenType.INTEGER, new BigInteger(String.valueOf(keyValue)))));
                } else {
                    throw new CdcException("Cannot get key column value from sql or generatedKey, key column: " + keyColumn + ". ");
                }
            } else {
                keyValue = value.longValue();
                keyValueList.add(keyValue);
                executableSqlWithKeyValueList.add(executableSql);
            }
        }
        generatedKeys.close();
        return new BinaryResult<>(executableSqlWithKeyValueList, keyValueList);
    }

    private Map<Long, Map<String, Token>> queryKeyColumnTokenMap(
            Statement statement, List<Long> keyValueList, List<String> columnList) throws SQLException {
        Map<Long, Map<String, Token>> map = new HashMap<>();
        if (columnList.isEmpty()) {
            return map;
        }
        List<String> resultColumnList = new ArrayList<>(columnList);
        resultColumnList.add(keyColumn);
        List<List<Long>> listList = BEListUtil.splitList(keyValueList, IN_SIZE);
        for (List<Long> item : listList) {
            int size = item.size();
            String keyInList = longListToString(item);
            String sql = "select "
                    + String.join(",", resultColumnList) + " "
                    + "from " + tableName + " "
                    + "where " + keyColumn + " "
                    + "in (" + keyInList + ")";
            ResultSet resultSet = statement.executeQuery(sql);
            Map<String, Token> columnTokenMap = new HashMap<>();
            int i = 0;
            while (resultSet.next()) {
                Long keyValue = null;
                for (String column : resultColumnList) {
                    if (keyColumn.equals(column)) {
                        keyValue = resultSet.getLong(column);
                        columnTokenMap.put(column, dataFormatterRegistry.parse(keyValue));
                    } else {
                        columnTokenMap.put(column, dataFormatterRegistry.parse(resultSet.getObject(column)));
                    }
                }
                map.put(keyValue, columnTokenMap);
                i++;
            }
            resultSet.close();
            if (size != i) {
                throw new CdcException("Cannot get enough key value from resultSet. keyValueList: " + keyInList + ".");
            }
        }
        return map;
    }

    public <T> T executeDelete(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        // -------------------------------------
        //  对于Statement模式来说，记录下来SQL就行了
        // -------------------------------------
        if (checkTableMode(TableConfig.CM_STATEMENT)) {
            List<String> statementCdcSqlList = new ArrayList<>(executableSqlList);
            executeCdcSql(TableConfig.CM_STATEMENT, statementCdcSqlList);
        }

        // ----------------------------------
        //  对于ROW模式，需要记录下来具体删除的行。
        // ----------------------------------
        if (checkTableMode(TableConfig.CM_ROW)) {
            List<String> rowCdcSqlList = new ArrayList<>();
            // 多余delete语句来说，在batch模式下，如果sqlX和sqlY同时命中了KeyN，则只应该记录一次。
            Set<Long> keyValueSet = new LinkedHashSet<>();
            try (Statement statement = cdcConnection.getDelegate().createStatement()) {
                for (String executableSql : executableSqlList) {
                    // select 获取主键值。
                    keyValueSet.addAll(lockKey(statement, executableSql));
                }
            }
            // 对于simple模式 ： 1:n -> 1:1 优化；
            // 对于batch模式，也进行了： 1:1 -> n:1 优化。
            if (!keyValueSet.isEmpty()) {
                List<List<Long>> listList = BEListUtil.splitList(new ArrayList<>(keyValueSet), IN_SIZE);
                for (List<Long> item : listList) {
                    rowCdcSqlList.add("delete from " + tableName + " where " + keyColumn + " in (" + longListToString(item) + ")");
                }
            }
            executeCdcSql(TableConfig.CM_ROW, rowCdcSqlList);
        }

        return pi.invoke();
    }

    public <T> T executeUpdate(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        assertStableUpdateSql(parameterizedSql, keyColumn);

        // -------------------------------------
        //  对于Statement模式来说，记录下来SQL就行了
        // -------------------------------------
        if (checkTableMode(TableConfig.CM_STATEMENT)) {
            List<String> statementCdcSqlList = new ArrayList<>(executableSqlList);
            executeCdcSql(TableConfig.CM_STATEMENT, statementCdcSqlList);
        }

        // ----------------------------------
        //  对于ROW模式，需要记录下来具体更新的行。
        // ----------------------------------
        if (checkTableMode(TableConfig.CM_ROW)) {
            List<String> rowCdcSqlList = new ArrayList<>();
            List<String> columnList = SqlUtils.extractNonLiteralColumnFromUpdateOrInsertSql(parameterizedSql);
            try (Statement keyValueStatement = cdcConnection.getDelegate().createStatement()) {
                for (String executableSql : executableSqlList) {
                    // select 获取主键值。
                    List<Long> keyValueList = lockKey(keyValueStatement, executableSql);

                    // 进行 1:n -> 1:1 优化
                    if (columnList.isEmpty()) {
                        List<List<Long>> listList = BEListUtil.splitList(keyValueList, IN_SIZE);
                        for (List<Long> item : listList) {
                            String rowCdcSql = SqlUtils.replaceDmlWhereSeg(executableSql, "where " + keyColumn + " in (" + longListToString(item) + ")");
                            rowCdcSqlList.add(SqlUtils.updateOrInsertUpdateColumnValue(rowCdcSql, Collections.EMPTY_MAP));
                        }
                    }
                    // 无法进行 1:n -> 1:1 优化
                    else {
                        try (Statement statement = cdcConnection.getDelegate().createStatement()) {
                            Map<Long, Map<String, Token>> keyColumnTokenMap = queryKeyColumnTokenMap(statement, keyValueList, columnList);
                            for (Long keyValue : keyValueList) {
                                String rowCdcSql = SqlUtils.replaceDmlWhereSeg(executableSql, "where " + keyColumn + " = " + keyValue);
                                Map<String, Token> columnTokenMap = keyColumnTokenMap.get(keyValue);
                                rowCdcSqlList.add(SqlUtils.updateOrInsertUpdateColumnValue(rowCdcSql, columnTokenMap));
                            }
                        }
                    }
                }
            }

            // 1:1 -> n:1 优化
            List<String> tmpSqlList = new ArrayList<>(rowCdcSqlList);
            rowCdcSqlList.clear();
            List<Long> sameUpdateSegKeyValueList = new ArrayList<>();
            String preUpdateSeg = null;
            for (String rowCdcSql : tmpSqlList) {
                BinaryResult<String, List<Long>> binaryResult = SqlUtils.separateUpdateSegAndKeyValues(rowCdcSql);
                String curUpdateSeg = binaryResult.getFirstResult();
                if (preUpdateSeg == null || preUpdateSeg.equals(curUpdateSeg)) {
                    sameUpdateSegKeyValueList.addAll(binaryResult.getSecondResult());
                } else {
                    rowCdcSqlList.addAll(assembleRowUpdateSqlList(preUpdateSeg, sameUpdateSegKeyValueList));
                    sameUpdateSegKeyValueList.clear();
                    sameUpdateSegKeyValueList.addAll(binaryResult.getSecondResult());
                }
                preUpdateSeg = curUpdateSeg;
            }
            rowCdcSqlList.addAll(assembleRowUpdateSqlList(preUpdateSeg, sameUpdateSegKeyValueList));

            executeCdcSql(TableConfig.CM_ROW, rowCdcSqlList);
        }

        return pi.invoke();
    }

    private List<String> assembleRowUpdateSqlList(String updateSeg, List<Long> keyValueList) {
        List<String> rowCdcSqlList = new ArrayList<>();
        List<List<Long>> listList = BEListUtil.splitList(keyValueList, IN_SIZE);
        listList.forEach(item -> rowCdcSqlList.add(updateSeg + " where " + keyColumn + " in (" + longListToString(item) + ")"));
        return rowCdcSqlList;
    }


    /**
     * @param statement
     * @param executableSql
     * @return executableSql 执行时锁住的key
     * @throws SQLException
     */
    private List<Long> lockKey(Statement statement, String executableSql) throws SQLException {
        List<Long> keyValueList = new ArrayList<>();
        ResultSet resultSet = statement.executeQuery(assembleSelectKeyValueSql(executableSql));
        while (resultSet.next()) {
            keyValueList.add(resultSet.getLong(keyColumn));
        }
        resultSet.close();
        return keyValueList;
    }

    private String assembleSelectKeyValueSql(String executableSql) {
        return "select "
                + keyColumn + " "
                + "from "
                + tableName + " "
                + SqlUtils.extractWhereConditionFromUpdateOrDeleteSql(executableSql) + " "
                + "for update";
    }

    private void assertStableUpdateSql(String sql, String keyColumn) {
        if (!SqlUtils.checkStableUpdateSql(sql, keyColumn)) {
            throw new CdcException("update sql cannot update column which in where-condition and primary key column. ");
        }
    }

    private void executeCdcSql(String cdcMode, List<String> cdcSqlList) throws SQLException {
        if (cdcSqlList.isEmpty()) {
            return;
        }

        List<String> sqlList = new ArrayList<>();
        String sqlPreSegment = "insert into " + tableName + "_cdc_" + cdcMode + "(executable_sql) values";
        List<List<String>> listList = BEListUtil.splitList(cdcSqlList, EXECUTE_INSERT_SQL_BATCH);
        String aloneSql = null;
        for (List<String> item : listList) {
            StringBuilder sql = new StringBuilder(sqlPreSegment);
            for (String string : item) {
                sql.append("('").append(string.replace("'", "\\'")).append("'),");
            }
            if (item.size() == EXECUTE_INSERT_SQL_BATCH) {
                sqlList.add(sql.substring(0, sql.length() - 1));
            } else {
                aloneSql = sql.substring(0, sql.length() - 1);
            }
        }

        Connection connection = cdcConnection.getDelegate();
        try (Statement statement = connection.createStatement()) {
            for (int i = 0; i < sqlList.size(); i++) {
                statement.addBatch(sqlList.get(i));
                if (i != 0 && i % EXECUTE_INSERT_STATEMENT_BATCH == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
        }
        if (aloneSql != null) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(aloneSql);
            }
        }
    }

    public boolean checkTableMode(String mode) {
        if (tableConfig == null) {
            return false;
        }
        List<String> cdcModeList = tableConfig.getCdcModeList();
        return cdcModeList.contains(mode);
    }

    private String longListToString(List<Long> longList) {
        return longList.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

}
