package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.asynctask.BinaryResult;
import cn.addenda.businesseasy.cdc.sql.SqlUtils;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author addenda
 * @datetime 2022/9/24 9:51
 */
public class InsertPsDelegate extends AbstractPsDelegate {

    public InsertPsDelegate(CdcConnection cdcConnection, PreparedStatement ps, TableConfig tableConfig, String parameterizedSql) {
        super(cdcConnection, ps, tableConfig, parameterizedSql);
    }

    @Override
    public <T> T execute(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
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

}
