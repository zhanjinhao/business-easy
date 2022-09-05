package cn.addenda.businesseasy.cdc.sql;

import cn.addenda.businesseasy.cdc.CdcException;
import cn.addenda.ro.grammar.ast.CurdParser;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.create.Insert;
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
            if (TokenType.WHERE.equals(token.getType())) {
                wherePreTokenFg = true;
            }
            if (wherePreTokenFg && !TokenType.EOF.equals(token.getType())) {
                sb.append(token.getLiteral()).append(" ");
            }
        }
        return !wherePreTokenFg ? "" : sb.toString();
    }

    public static boolean checkStableUpdateSql(String sql, String keyColumn) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Update update = (Update) curdParser.parse();

        // where 里的条件列
        WhereSeg whereSeg = (WhereSeg) update.getWhereSeg();
        Set<Token> updatedColumnList = new HashSet<>();
        if (whereSeg != null) {
            updatedColumnList = whereSeg.accept(WhereSegConditionColumnVisitor.getInstance());
        }
        Set<String> updatedColumnNameLis = updatedColumnList
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

    public static boolean checkPlainValueInUpdateOrInsert(String sql) {
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
                    Curd value = entry.getValue();
                    if (Boolean.FALSE.equals(value.accept(BinaryArithmeticPlainValueVisitor.getInstance()))) {
                        return false;
                    }
                }
            } else if (insertRep instanceof InsertValuesRep) {
                InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                List<Curd> curdList = curdListList.get(0);
                for (Curd curd : curdList) {
                    if (Boolean.FALSE.equals(curd.accept(BinaryArithmeticPlainValueVisitor.getInstance()))) {
                        return false;
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
                if (Boolean.FALSE.equals(value.accept(BinaryArithmeticPlainValueVisitor.getInstance()))) {
                    return false;
                }
            }
        }

        return true;
    }

}
