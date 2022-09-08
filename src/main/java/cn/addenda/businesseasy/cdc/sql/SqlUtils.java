package cn.addenda.businesseasy.cdc.sql;

import cn.addenda.businesseasy.cdc.CdcException;
import cn.addenda.ro.grammar.ast.CurdParser;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.create.InsertSelectRep;
import cn.addenda.ro.grammar.ast.create.InsertSetRep;
import cn.addenda.ro.grammar.ast.create.InsertValuesRep;
import cn.addenda.ro.grammar.ast.expression.AssignmentList;
import cn.addenda.ro.grammar.ast.expression.Curd;
import cn.addenda.ro.grammar.ast.expression.Literal;
import cn.addenda.ro.grammar.ast.expression.WhereSeg;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.scan.DefaultScanner;
import cn.addenda.ro.grammar.lexical.scan.TokenSequence;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Insert insert = (Insert) curdParser.parse();
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
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Update update = (Update) curdParser.parse();

        // where 里的条件列
        WhereSeg whereSeg = (WhereSeg) update.getWhereSeg();
        Set<Token> conditionColumnList = new HashSet<>();
        if (whereSeg != null) {
            conditionColumnList = whereSeg.accept(WhereSegConditionColumnVisitor.getInstance());
        }
        Set<String> updatedColumnNameLis = conditionColumnList
                .stream().map(item -> String.valueOf(item.getLiteral())).collect(Collectors.toSet());

        // where 里的条件列 和 key列 不能在更新列里面出现
        AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
        List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
        for (AssignmentList.Entry entry : entryList) {
            String columnName = String.valueOf(entry.getColumn().getLiteral());
            if (keyColumn.equals(columnName) || updatedColumnNameLis.contains(columnName)) {
                return false;
            }
        }
        return true;
    }

    public static List<String> extractNonLiteralColumnFromUpdateOrDeleteSql(String sql) {
        if (!isInsertSql(sql) && !isUpdateSql(sql)) {
            throw new CdcException("only support insert and update sql. ");
        }

        List<String> columnNameList = new ArrayList<>();
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (parse instanceof Insert) {
            Insert insert = (Insert) parse;
            Curd insertRep = insert.getInsertRep();
            if (insertRep instanceof InsertSetRep) {
                InsertSetRep insertSetRep = (InsertSetRep) insertRep;
                AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
                List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
                for (AssignmentList.Entry entry : entryList) {
                    if (!(entry.getValue() instanceof Literal)) {
                        columnNameList.add(String.valueOf(entry.getColumn().getLiteral()));
                    }
                }
            } else if (insertRep instanceof InsertValuesRep) {
                InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                List<Curd> curdList = curdListList.get(0);
                List<Token> columnList = insertValuesRep.getColumnList();
                for (int i = 0; i < curdList.size(); i++) {
                    Curd curd = curdList.get(i);
                    if (!(curd instanceof Literal)) {
                        columnNameList.add(String.valueOf(columnList.get(i).getLiteral()));
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
                if (!(entry.getValue() instanceof Literal)) {
                    columnNameList.add(String.valueOf(entry.getColumn().getLiteral()));
                }
            }
        }

        return columnNameList;
    }

    public static String insertInjectColumnValue(String sql, String keyColumn, Long firstResult) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Insert insert = (Insert) curdParser.parse();
        Curd insertRep = insert.getInsertRep();
        if (insertRep instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) insertRep;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            entryList.add(new AssignmentList.Entry(new Token(TokenType.IDENTIFIER, keyColumn),
                    new Literal(new Token(TokenType.INTEGER, new BigInteger(String.valueOf(firstResult))))));
        } else if (insertRep instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
            List<Token> columnList = insertValuesRep.getColumnList();
            columnList.add(new Token(TokenType.IDENTIFIER, keyColumn));
            List<Curd> curdList = insertValuesRep.getCurdListList().get(0);
            curdList.add(new Literal(new Token(TokenType.INTEGER, new BigInteger(String.valueOf(firstResult)))));
        } else if (insertRep instanceof InsertSelectRep) {
            throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
        }

        return insert.toString();
    }


    public static String UpdateOrInsertUpdateColumnValue(String sql, Map<String, Object> columnValueMap) {
        if (!isInsertSql(sql) && !isUpdateSql(sql)) {
            throw new CdcException("only support insert and update sql. ");
        }

        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (parse instanceof Insert) {
            Insert insert = (Insert) parse;
            Curd insertRep = insert.getInsertRep();
            if (insertRep instanceof InsertSetRep) {
                InsertSetRep insertSetRep = (InsertSetRep) insertRep;
                AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
                List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
                for (AssignmentList.Entry entry : entryList) {
                    String columnName = String.valueOf(entry.getColumn().getLiteral());
                    if (columnValueMap.containsKey(columnName)) {
                        Token token = parseObjectToToken(columnValueMap.get(columnName));
                        entry.setValue(new Literal(token));
                    }
                }
            } else if (insertRep instanceof InsertValuesRep) {
                InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                List<Curd> curdList = curdListList.get(0);
                List<Token> columnList = insertValuesRep.getColumnList();
                for (int i = 0; i < columnList.size(); i++) {
                    String columnName = String.valueOf(columnList.get(i).getLiteral());
                    if (columnValueMap.containsKey(columnName)) {
                        Token token = parseObjectToToken(columnValueMap.get(columnName));
                        curdList.set(i, new Literal(token));
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
                if (columnValueMap.containsKey(columnName)) {
                    Token token = parseObjectToToken(columnValueMap.get(columnName));
                    entry.setValue(new Literal(token));
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

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    private static final ThreadLocal<SimpleDateFormat> TIME_FORMAT_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm:ss"));
    private static final ThreadLocal<SimpleDateFormat> DATETIME_FORMAT_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String SINGLE_QUOTATION = "'";

    public static String parseObjectToString(Object obj) {
        assertDataType(obj);
        if (obj == null) {
            return "null";
        } else if (obj instanceof Boolean) {
            return Boolean.FALSE.equals(obj) ? "false" : "true";
        } else if (obj instanceof Double) {
            return obj.toString();
        } else if (obj instanceof Float) {
            return obj.toString();
        } else if (obj instanceof Long) {
            return obj.toString();
        } else if (obj instanceof Integer) {
            return obj.toString();
        } else if (obj instanceof Short) {
            return obj.toString();
        } else if (obj instanceof Byte) {
            return obj.toString();
        } else if (obj instanceof Character) {
            return SINGLE_QUOTATION + obj + SINGLE_QUOTATION;
        } else if (obj instanceof CharSequence) {
            return SINGLE_QUOTATION + obj + SINGLE_QUOTATION;
        } else if (obj instanceof BigDecimal) {
            return obj.toString();
        } else if (obj instanceof BigInteger) {
            return obj.toString();
        } else if (obj instanceof java.sql.Date) {
            return SINGLE_QUOTATION + DATE_FORMAT_THREAD_LOCAL.get().format((java.sql.Date) obj) + SINGLE_QUOTATION;
        } else if (obj instanceof Time) {
            return SINGLE_QUOTATION + TIME_FORMAT_THREAD_LOCAL.get().format((Time) obj) + SINGLE_QUOTATION;
        } else if (obj instanceof Timestamp) {
            return SINGLE_QUOTATION + DATETIME_FORMAT_THREAD_LOCAL.get().format((java.util.Date) obj) + SINGLE_QUOTATION;
        } else if (obj instanceof java.util.Date) {
            return SINGLE_QUOTATION + DATETIME_FORMAT_THREAD_LOCAL.get().format((java.util.Date) obj) + SINGLE_QUOTATION;
        } else if (obj instanceof LocalDateTime) {
            return SINGLE_QUOTATION + DATE_TIME_FORMATTER.format((LocalDateTime) obj) + SINGLE_QUOTATION;
        } else if (obj instanceof LocalDate) {
            return SINGLE_QUOTATION + DATE_FORMATTER.format((LocalDate) obj) + SINGLE_QUOTATION;
        } else if (obj instanceof LocalTime) {
            return SINGLE_QUOTATION + TIME_FORMATTER.format((LocalTime) obj) + SINGLE_QUOTATION;
        }
        return null;
    }

    public static Token parseObjectToToken(Object obj) {
        assertDataType(obj);
        if (obj == null) {
            return new Token(TokenType.NULL, "null");
        } else if (obj instanceof Boolean) {
            return Boolean.FALSE.equals(obj) ? new Token(TokenType.FALSE, "false") : new Token(TokenType.TRUE, "true");
        } else if (obj instanceof Double) {
            return new Token(TokenType.DECIMAL, new BigDecimal(obj.toString()));
        } else if (obj instanceof Float) {
            return new Token(TokenType.DECIMAL, new BigDecimal(obj.toString()));
        } else if (obj instanceof Long) {
            return new Token(TokenType.INTEGER, new BigInteger(obj.toString()));
        } else if (obj instanceof Integer) {
            return new Token(TokenType.INTEGER, new BigInteger(obj.toString()));
        } else if (obj instanceof Short) {
            return new Token(TokenType.INTEGER, new BigInteger(obj.toString()));
        } else if (obj instanceof Byte) {
            return new Token(TokenType.INTEGER, new BigInteger(obj.toString()));
        } else if (obj instanceof Character) {
            return new Token(TokenType.STRING, obj.toString());
        } else if (obj instanceof CharSequence) {
            return new Token(TokenType.STRING, obj);
        } else if (obj instanceof BigDecimal) {
            return new Token(TokenType.DECIMAL, obj.toString());
        } else if (obj instanceof BigInteger) {
            return new Token(TokenType.INTEGER, obj.toString());
        } else if (obj instanceof java.sql.Date) {
            return new Token(TokenType.STRING, DATE_FORMAT_THREAD_LOCAL.get().format((java.sql.Date) obj));
        } else if (obj instanceof Time) {
            return new Token(TokenType.STRING, TIME_FORMAT_THREAD_LOCAL.get().format((Time) obj));
        } else if (obj instanceof Timestamp) {
            return new Token(TokenType.STRING, DATETIME_FORMAT_THREAD_LOCAL.get().format((java.util.Date) obj));
        } else if (obj instanceof java.util.Date) {
            return new Token(TokenType.STRING, DATETIME_FORMAT_THREAD_LOCAL.get().format((java.util.Date) obj));
        } else if (obj instanceof LocalDateTime) {
            return new Token(TokenType.STRING, DATE_TIME_FORMATTER.format((LocalDateTime) obj));
        } else if (obj instanceof LocalDate) {
            return new Token(TokenType.STRING, DATE_FORMATTER.format((LocalDate) obj));
        } else if (obj instanceof LocalTime) {
            return new Token(TokenType.STRING, TIME_FORMATTER.format((LocalTime) obj));
        }
        return null;
    }

    public static void assertDataType(Object object) {
        if (object == null) {
            return;
        }
        if (object instanceof Boolean || object instanceof Double || object instanceof Float || object instanceof Long ||
                object instanceof Integer || object instanceof Short || object instanceof Byte || object instanceof Character ||
                object instanceof CharSequence || object instanceof BigDecimal || object instanceof BigInteger || object instanceof Date ||
                object instanceof LocalDateTime || object instanceof LocalDate || object instanceof LocalTime) {
            return;
        }
        throw new CdcException("不支持的数据类型，仅支持：null, Boolean, Double, Float, Long, Integer, Short, Byte, Character, CharSequence, BigDecimal, BigInteger, Date, LocalDateTime, LocalDate, LocalTime. ");
    }

}
