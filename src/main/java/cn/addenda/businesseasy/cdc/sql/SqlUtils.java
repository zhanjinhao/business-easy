package cn.addenda.businesseasy.cdc.sql;

import cn.addenda.businesseasy.asynctask.BinaryResult;
import cn.addenda.businesseasy.cdc.CdcException;
import cn.addenda.businesseasy.cdc.format.DataFormatterRegistry;
import cn.addenda.ec.calculator.CalculatorFactory;
import cn.addenda.ec.function.calculator.FunctionCalculator;
import cn.addenda.ro.grammar.ast.CurdUtils;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.create.InsertSelectRep;
import cn.addenda.ro.grammar.ast.create.InsertSetRep;
import cn.addenda.ro.grammar.ast.create.InsertValuesRep;
import cn.addenda.ro.grammar.ast.expression.AssignmentList;
import cn.addenda.ro.grammar.ast.expression.Comparison;
import cn.addenda.ro.grammar.ast.expression.Curd;
import cn.addenda.ro.grammar.ast.expression.InCondition;
import cn.addenda.ro.grammar.ast.expression.Literal;
import cn.addenda.ro.grammar.ast.expression.WhereSeg;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.scan.DefaultScanner;
import cn.addenda.ro.grammar.lexical.scan.TokenSequence;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/8/27 10:50
 */
public class SqlUtils {

    private SqlUtils() {
    }

    public static boolean isInsertSql(String sql) {
        return sql.trim().toLowerCase(Locale.ROOT).startsWith("insert");
    }

    public static boolean isDeleteSql(String sql) {
        return sql.trim().toLowerCase(Locale.ROOT).startsWith("delete");
    }

    public static boolean isUpdateSql(String sql) {
        return sql.trim().toLowerCase(Locale.ROOT).startsWith("update");
    }

    public static boolean isSelectSql(String sql) {
        return sql.trim().toLowerCase(Locale.ROOT).startsWith("select");
    }

    /**
     * 从 DML SQL 里面提取出来表名。
     * DQL 语句抛出异常。
     */
    public static String extractTableNameFromDmlSql(String sql) {
        if (isSelectSql(sql)) {
            throw new CdcException("only support dml sql. ");
        }
        TokenSequence tokenSequence = new DefaultScanner(sql).scanTokens();
        List<Token> source = tokenSequence.getSource();
        boolean tableNamePreTokenFg = false;
        for (Token token : source) {
            if (TokenType.UPDATE.equals(token.getType()) ||
                TokenType.INTO.equals(token.getType()) ||
                TokenType.FROM.equals(token.getType())) {
                tableNamePreTokenFg = true;
                continue;
            }
            if (tableNamePreTokenFg) {
                return String.valueOf(token.getLiteral());
            }
        }
        return null;
    }

    /**
     * 从 Insert SQL 里面提取出来字段名。
     * 其他语句抛出异常。
     */
    public static <T> T extractColumnValueFromInsertSql(String sql, String keyColumn, Class<T> clazz) {
        if (!isInsertSql(sql)) {
            throw new CdcException("only support insert sql. ");
        }
        Insert insert = CurdUtils.parseInsert(sql, false);
        Curd insertRep = insert.getInsertRep();
        if (insertRep instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) insertRep;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            for (AssignmentList.Entry entry : entryList) {
                if (keyColumn.equals(entry.getColumn().getLiteral())) {
                    Curd value = entry.getValue();
                    if (!(value instanceof Literal)) {
                        throw new CdcException("keyColumn must be Long or long data type");
                    }
                    Literal literal = (Literal) value;
                    Token result = literal.getValue();
                    if (clazz.isAssignableFrom(result.getLiteral().getClass())) {
                        return (T) result.getLiteral();
                    }
                }
            }
        } else if (insertRep instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
            List<Token> columnList = insertValuesRep.getColumnList();
            int index = -1;
            for (int i = 0; i < columnList.size(); i++) {
                Token token = columnList.get(i);
                if (keyColumn.equals(token.getLiteral())) {
                    index = i;
                }
            }
            if (index != -1) {
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                Literal literal = (Literal) curdListList.get(0).get(index);
                Token result = literal.getValue();
                if (clazz.isAssignableFrom(result.getLiteral().getClass())) {
                    return (T) result.getLiteral();
                }
            }
        } else {
            throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
        }
        return null;
    }

    /**
     * 从 Update or Delete SQL 里面提取出来 where 条件。
     * 其他语句抛出异常。
     */
    public static String extractWhereConditionFromUpdateOrDeleteSql(String sql) {
        if (!isUpdateSql(sql) && !isDeleteSql(sql)) {
            throw new CdcException("only support update or delete sql. ");
        }
        TokenSequence tokenSequence = new DefaultScanner(sql).scanTokens();
        List<Token> source = tokenSequence.getSource();
        StringBuilder sb = new StringBuilder();
        boolean wherePreTokenFg = false;
        for (Token token : source) {
            TokenType type = token.getType();
            if (TokenType.WHERE.equals(type)) {
                wherePreTokenFg = true;
            }
            Object literal = token.getLiteral();
            if (wherePreTokenFg && !TokenType.EOF.equals(type)) {
                if (TokenType.STRING.equals(type)) {
                    sb.append(" '").append(literal).append("'");
                } else {
                    // mysql 的函数 支持 now(), now( )写法，但是不支持 now (), now ( ) 写法。所以(前不留空格
                    if (!("(".equals(literal))) {
                        sb.append(" ");
                    }
                    sb.append(literal);
                }
            }
        }
        return !wherePreTokenFg ? "" : sb.toString();
    }

    public static boolean checkStableUpdateSql(String sql, String keyColumn) {
        Update update = CurdUtils.parseUpdate(sql, false);

        // where 里的条件列
        WhereSeg whereSeg = (WhereSeg) update.getWhereSeg();
        Set<Token> conditionColumnList = new HashSet<>();
        if (whereSeg != null) {
            conditionColumnList = whereSeg.accept(WhereSegColumnVisitor.getInstance());
        }
        Set<String> conditionColumnNameList = conditionColumnList
            .stream().map(item -> String.valueOf(item.getLiteral())).collect(Collectors.toSet());

        // where 里的条件列 和 key列 不能在更新列里面出现
        AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
        List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
        for (AssignmentList.Entry entry : entryList) {
            String columnName = String.valueOf(entry.getColumn().getLiteral());
            if (keyColumn.equals(columnName) || conditionColumnNameList.contains(columnName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return first dependentColumnNameList; second calculableColumnNameList
     */
    public static BinaryResult<List<String>, List<BinaryResult<String, Curd>>> divideColumnFromUpdateOrInsertSql(String sql) {
        if (!isInsertSql(sql) && !isUpdateSql(sql)) {
            throw new CdcException("only support insert and update sql. ");
        }

        List<String> dependentColumnNameList = new ArrayList<>();
        List<BinaryResult<String, Curd>> calculableColumnNameList = new ArrayList<>();

        Curd parse = CurdUtils.parse(sql, false);
        if (parse instanceof Insert) {
            Insert insert = (Insert) parse;
            Curd insertRep = insert.getInsertRep();
            if (insertRep instanceof InsertSetRep) {
                InsertSetRep insertSetRep = (InsertSetRep) insertRep;
                AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
                List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
                for (AssignmentList.Entry entry : entryList) {
                    Curd value = entry.getValue();
                    String columnName = String.valueOf(entry.getColumn().getLiteral());
                    if (value instanceof Literal) {
                        // no-op
                    } else if (Boolean.TRUE.equals(value.accept(InsertOrUpdateCalculableColumnVisitor.getInstance()))) {
                        calculableColumnNameList.add(new BinaryResult<>(columnName, value));
                    } else {
                        dependentColumnNameList.add(columnName);
                    }
                }
            } else if (insertRep instanceof InsertValuesRep) {
                InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                List<Curd> curdList = curdListList.get(0);
                List<Token> columnList = insertValuesRep.getColumnList();
                for (int i = 0; i < curdList.size(); i++) {
                    Curd value = curdList.get(i);
                    String columnName = String.valueOf(columnList.get(i));
                    if (value instanceof Literal) {
                        // no-op
                    } else if (value.accept(InsertOrUpdateCalculableColumnVisitor.getInstance())) {
                        calculableColumnNameList.add(new BinaryResult<>(columnName, value));
                    } else {
                        dependentColumnNameList.add(columnName);
                    }
                }
            } else {
                throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
            }
        } else if (parse instanceof Update) {
            Update update = (Update) parse;
            AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            for (AssignmentList.Entry entry : entryList) {
                Curd value = entry.getValue();
                String columnName = String.valueOf(entry.getColumn().getLiteral());
                if (value instanceof Literal) {
                    // no-op
                } else if (value.accept(InsertOrUpdateCalculableColumnVisitor.getInstance())) {
                    calculableColumnNameList.add(new BinaryResult<>(columnName, value));
                } else {
                    dependentColumnNameList.add(columnName);
                }
            }
        }

        return new BinaryResult<>(dependentColumnNameList, calculableColumnNameList);
    }

    public static boolean checkInsertMultipleRows(String insertSql) {
        Insert insert = CurdUtils.parseInsert(insertSql, false);
        Curd insertRep = insert.getInsertRep();
        if (insertRep instanceof InsertSetRep) {
            return false;
        } else if (insertRep instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            return curdListList.size() > 1;
        } else {
            throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
        }
    }

    public static List<String> splitInsertMultipleRows(String multipleRowsInsertSql) {
        List<String> singleRowList = new ArrayList<>();
        Insert insert = CurdUtils.parseInsert(multipleRowsInsertSql, false);
        Curd insertRep = insert.getInsertRep();
        if (insertRep instanceof InsertSetRep) {
            singleRowList.add(multipleRowsInsertSql);
        } else if (insertRep instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
            List<Token> columnList = insertValuesRep.getColumnList();
            String prefix = "insert into "
                + insert.getTableName().getLiteral() + "(" + columnList.stream().map(item -> String.valueOf(item.getLiteral())).collect(Collectors.joining(","))
                + ") values ";
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            for (List<Curd> curdList : curdListList) {
                String sql = prefix + curdList.stream().map(Curd::toString).collect(Collectors.joining(",", "(", ")"));
                singleRowList.add(sql);
            }
        } else {
            throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
        }
        return singleRowList;
    }

    public static String insertInjectColumnValue(String sql, String keyColumn, Token token) {
        Insert insert = CurdUtils.parseInsert(sql, false);
        Curd insertRep = insert.getInsertRep();
        if (insertRep instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) insertRep;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            entryList.add(new AssignmentList.Entry(new Token(TokenType.IDENTIFIER, keyColumn), new Literal(token)));
        } else if (insertRep instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
            List<Token> columnList = insertValuesRep.getColumnList();
            columnList.add(new Token(TokenType.IDENTIFIER, keyColumn));
            List<Curd> curdList = insertValuesRep.getCurdListList().get(0);
            curdList.add(new Literal(token));
        } else if (insertRep instanceof InsertSelectRep) {
            throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
        }

        return insert.toString();
    }


    public static String updateOrInsertUpdateColumnValue(String sql, Map<String, Token> columnTokenMap) {
        if (!isInsertSql(sql) && !isUpdateSql(sql)) {
            throw new CdcException("only support insert and update sql. ");
        }

        Curd parse = CurdUtils.parse(sql, false);
        if (parse instanceof Insert) {
            Insert insert = (Insert) parse;
            Curd insertRep = insert.getInsertRep();
            if (insertRep instanceof InsertSetRep) {
                InsertSetRep insertSetRep = (InsertSetRep) insertRep;
                AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
                List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
                for (AssignmentList.Entry entry : entryList) {
                    String columnName = String.valueOf(entry.getColumn().getLiteral());
                    if (columnTokenMap.containsKey(columnName)) {
                        entry.setValue(new Literal(columnTokenMap.get(columnName)));
                    }
                }
            } else if (insertRep instanceof InsertValuesRep) {
                InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                List<Curd> curdList = curdListList.get(0);
                List<Token> columnList = insertValuesRep.getColumnList();
                for (int i = 0; i < columnList.size(); i++) {
                    String columnName = String.valueOf(columnList.get(i).getLiteral());
                    if (columnTokenMap.containsKey(columnName)) {
                        curdList.set(i, new Literal(columnTokenMap.get(columnName)));
                    }
                }
            } else {
                throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
            }
        } else if (parse instanceof Update) {
            Update update = (Update) parse;
            AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            for (AssignmentList.Entry entry : entryList) {
                String columnName = String.valueOf(entry.getColumn().getLiteral());
                if (columnTokenMap.containsKey(columnName)) {
                    entry.setValue(new Literal(columnTokenMap.get(columnName)));
                }
            }
        }

        return parse.toString();
    }


    public static String updateOrInsertCalculateColumnValue(String sql, List<String> calculableColumnList, DataFormatterRegistry dataFormatterRegistry, FunctionCalculator functionCalculator) {
        if (!isInsertSql(sql) && !isUpdateSql(sql)) {
            throw new CdcException("only support insert and update sql. ");
        }

        Curd parse = CurdUtils.parse(sql, false);
        if (parse instanceof Insert) {
            Insert insert = (Insert) parse;
            Curd insertRep = insert.getInsertRep();
            if (insertRep instanceof InsertSetRep) {
                InsertSetRep insertSetRep = (InsertSetRep) insertRep;
                AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
                List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
                for (AssignmentList.Entry entry : entryList) {
                    String columnName = String.valueOf(entry.getColumn().getLiteral());
                    if (calculableColumnList.contains(columnName)) {
                        Object result = CalculatorFactory.createExpressionCalculator(entry.getValue(), functionCalculator).calculate();
                        entry.setValue(new Literal(dataFormatterRegistry.parse(result)));
                    }
                }
            } else if (insertRep instanceof InsertValuesRep) {
                InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                List<Curd> curdList = curdListList.get(0);
                List<Token> columnList = insertValuesRep.getColumnList();
                for (int i = 0; i < columnList.size(); i++) {
                    String columnName = String.valueOf(columnList.get(i).getLiteral());
                    if (calculableColumnList.contains(columnName)) {
                        Object result = CalculatorFactory.createExpressionCalculator(curdList.get(i), functionCalculator).calculate();
                        curdList.set(i, new Literal(dataFormatterRegistry.parse(result)));
                    }
                }
            } else {
                throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
            }
        } else if (parse instanceof Update) {
            Update update = (Update) parse;
            AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            for (AssignmentList.Entry entry : entryList) {
                String columnName = String.valueOf(entry.getColumn().getLiteral());
                if (calculableColumnList.contains(columnName)) {
                    Object result = CalculatorFactory.createExpressionCalculator(entry.getValue(), functionCalculator).calculate();
                    entry.setValue(new Literal(dataFormatterRegistry.parse(result)));
                }
            }
        }

        return parse.toString();
    }


    public static String replaceDmlWhereSeg(String sql, String whereSeg) {
        if (!isUpdateSql(sql) && !isDeleteSql(sql)) {
            throw new CdcException("only support update or delete sql. ");
        }
        TokenSequence tokenSequence = new DefaultScanner(sql).scanTokens();
        List<Token> source = tokenSequence.getSource();
        StringBuilder sb = new StringBuilder();
        for (Token token : source) {
            TokenType type = token.getType();
            if (TokenType.WHERE.equals(type)) {
                break;
            }
            Object literal = token.getLiteral();
            if (TokenType.STRING.equals(type)) {
                sb.append(" '").append(literal).append("'");
            } else {
                // mysql 的函数 支持 now(), now( )写法，但是不支持 now (), now ( ) 写法。所以(前不留空格
                if (!("(".equals(literal))) {
                    sb.append(" ");
                }
                sb.append(literal);
            }
        }
        return sb + " " + whereSeg;
    }

    public static BinaryResult<String, List<Long>> separateUpdateSegAndKeyValues(String keyInOrKeyEqualConditionUpdateSql) {
        Update update = CurdUtils.parseUpdate(keyInOrKeyEqualConditionUpdateSql);

        List<Long> keyValueList = new ArrayList<>();
        WhereSeg whereSeg = (WhereSeg) update.getWhereSeg();
        Curd logic = whereSeg.getLogic();
        if (logic instanceof InCondition) {
            InCondition inCondition = (InCondition) logic;
            List<Curd> range = inCondition.getRange();
            for (Curd curd : range) {
                Token value = ((Literal) curd).getValue();
                keyValueList.add(((BigInteger) value.getLiteral()).longValue());
            }
        } else if (logic instanceof Comparison) {
            Comparison comparison = (Comparison) logic;
            Token value = ((Literal) comparison.getRightCurd()).getValue();
            keyValueList.add(((BigInteger) value.getLiteral()).longValue());
        } else {
            throw new CdcException("仅支持 key in 和 key equal 语法！");
        }

        int whereIndex = keyInOrKeyEqualConditionUpdateSql.indexOf("where");
        String updateSeg = keyInOrKeyEqualConditionUpdateSql.substring(0, whereIndex);
        return new BinaryResult<>(updateSeg, keyValueList);
    }

}
