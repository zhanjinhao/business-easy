package cn.addenda.businesseasy.fieldfilling.sql;

import cn.addenda.businesseasy.fieldfilling.FiledFillingException;
import cn.addenda.ro.grammar.ast.CurdParser;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.create.InsertSelectRep;
import cn.addenda.ro.grammar.ast.create.InsertSetRep;
import cn.addenda.ro.grammar.ast.create.InsertValuesRep;
import cn.addenda.ro.grammar.ast.delete.Delete;
import cn.addenda.ro.grammar.ast.retrieve.Select;
import cn.addenda.ro.grammar.ast.statement.*;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author ISJINHAO
 * @Date 2022/1/26 18:01
 */
public class FieldFillingSqlUtil {

    private static final Identifier defaultDeleteColumn = new Identifier(new Token(TokenType.IDENTIFIER, "del_fg"));
    private static final Literal defaultDeleteFlag = new Literal(new Token(TokenType.NUMBER, 1));
    private static final Literal defaultNonDeleteFlag = new Literal(new Token(TokenType.NUMBER, 0));
    private static final Comparison defaultDeleteComparison =
            new Comparison(defaultDeleteColumn, new Identifier(new Token(TokenType.EQUAL, "=")), defaultNonDeleteFlag);

    public static String selectAddComparison(String sql, Comparison comparison) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Select)) {
            return parse.toString();
        }
        if (comparison == null) {
            comparison = (Comparison) defaultDeleteComparison.deepClone();
        }
        return parse.accept(new CurdAddComparisonConditionVisitor(comparison)).toString();
    }

    public static String selectAddComparison(String sql, Comparison comparison, Set<String> tableNameSet) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Select)) {
            return parse.toString();
        }
        if (comparison == null) {
            comparison = (Comparison) defaultDeleteComparison.deepClone();
        }
        return parse.accept(new CurdAddComparisonConditionVisitor(comparison, tableNameSet)).toString();
    }

    public static String insertAddEntry(String sql, Map<String, Curd> entryMap) {
        return insertAddEntry(sql, entryMap, entryMap == null ? new HashSet<>() : entryMap.keySet());
    }

    public static String insertAddEntry(String sql, Map<String, Curd> entryMap, Set<String> duplicateCheck) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Insert)) {
            return parse.toString();
        }

        Insert insert = (Insert) parse;
        Curd curd = insert.getCurd();

        if (curd instanceof InsertSelectRep) {
            throw new FiledFillingException("不能对 InsertSelectRep 进行字段填充");
        } else if (curd instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) curd;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getEntryList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            entryList.forEach(item -> {
                String column = item.getColumnName().getLiteral().toString();
                if (duplicateCheck.contains(column)) {
                    throw new FiledFillingException("sql本身字段已存在" + column + "!");
                }
            });
            entryList.addAll(toEntry(entryMap));
        } else if (curd instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) curd;
            Set<String> strings = entryMap.keySet();
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            for (List<Curd> curdList : curdListList) {
                for (String key : strings) {
                    Curd literal = entryMap.get(key);
                    curdList.add(literal);
                }
            }
            List<Token> columnList = insertValuesRep.getColumnList();
            List<Token> collect = strings.stream().map(item -> new Token(TokenType.IDENTIFIER, item)).collect(Collectors.toList());
            columnList.addAll(collect);
        }

        return insert.toString();
    }


    public static String updateAddEntry(String sql, Map<String, Curd> entryMap) {
        return updateAddEntry(sql, entryMap == null ? new HashMap<>() : entryMap, entryMap == null ? new HashSet<>() : entryMap.keySet());
    }

    public static String updateAddEntry(String sql, Map<String, Curd> entryMap, Set<String> duplicateCheck) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Update)) {
            return parse.toString();
        }

        Update update = (Update) parse;
        AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
        List<AssignmentList.Entry> entryList = assignmentList.getEntryList();

        entryList.forEach(item -> {
            String column = item.getColumnName().getLiteral().toString();
            if (duplicateCheck.contains(column)) {
                throw new FiledFillingException("sql本身字段已存在" + column + "!");
            }
        });

        entryList.addAll(toEntry(entryMap));
        return update.toString();
    }

    private static Collection<? extends AssignmentList.Entry> toEntry(Map<String, Curd> entryMap) {
        List<AssignmentList.Entry> entryList = new ArrayList<>();
        Set<Map.Entry<String, Curd>> entries = entryMap.entrySet();
        for (Map.Entry<String, Curd> entry : entries) {
            String key = entry.getKey();
            Curd value = entry.getValue();
            entryList.add(new AssignmentList.Entry(new Token(TokenType.IDENTIFIER, key), value));
        }
        return entryList;
    }

    public static String deleteLogically(String sql, Token deleteColumn, Literal deleteLiteral) {
        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);
        Curd parse = curdParser.parse();
        if (!(parse instanceof Delete)) {
            return parse.toString();
        }

        Delete delete = (Delete) parse;
        Token tableName = delete.getTableName();
        Curd whereSeg = delete.getWhereSeg();
        List<AssignmentList.Entry> entryList = new ArrayList<>();
        if (deleteColumn == null) {
            deleteColumn = defaultDeleteColumn.getName().deepClone();
        }
        if (deleteLiteral == null) {
            deleteLiteral = (Literal) defaultDeleteFlag.deepClone();
        }
        entryList.add(new AssignmentList.Entry(deleteColumn, deleteLiteral));
        return new Update(tableName, new AssignmentList(entryList), whereSeg).toString();
    }

}
