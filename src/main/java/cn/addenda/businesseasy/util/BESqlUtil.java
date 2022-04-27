package cn.addenda.businesseasy.util;

import cn.addenda.businesseasy.fieldfilling.FiledFillingException;
import cn.addenda.ro.grammar.ast.AstMetaData;
import cn.addenda.ro.grammar.ast.CurdParser;
import cn.addenda.ro.grammar.ast.CurdParserFactory;
import cn.addenda.ro.grammar.ast.CurdVisitor;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.create.InsertSelectRep;
import cn.addenda.ro.grammar.ast.create.InsertSetRep;
import cn.addenda.ro.grammar.ast.create.InsertValuesRep;
import cn.addenda.ro.grammar.ast.create.OnDuplicateKey;
import cn.addenda.ro.grammar.ast.delete.Delete;
import cn.addenda.ro.grammar.ast.retrieve.CaseWhen;
import cn.addenda.ro.grammar.ast.retrieve.ColumnRep;
import cn.addenda.ro.grammar.ast.retrieve.ColumnSeg;
import cn.addenda.ro.grammar.ast.retrieve.ExistsCondition;
import cn.addenda.ro.grammar.ast.retrieve.GroupBySeg;
import cn.addenda.ro.grammar.ast.retrieve.GroupFunction;
import cn.addenda.ro.grammar.ast.retrieve.InCondition;
import cn.addenda.ro.grammar.ast.retrieve.LimitSeg;
import cn.addenda.ro.grammar.ast.retrieve.OrderBySeg;
import cn.addenda.ro.grammar.ast.retrieve.Select;
import cn.addenda.ro.grammar.ast.retrieve.SingleSelect;
import cn.addenda.ro.grammar.ast.retrieve.TableRep;
import cn.addenda.ro.grammar.ast.retrieve.TableSeg;
import cn.addenda.ro.grammar.ast.statement.*;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import cn.addenda.ro.grammar.util.ReflectUtils;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author ISJINHAO
 * @Date 2022/1/26 18:01
 */
public class BESqlUtil {

    private BESqlUtil() {
    }

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
        return insertAddEntry(sql, entryMap == null ? new HashMap<>() : entryMap, entryMap == null ? new HashSet<>() : entryMap.keySet());
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


    public static String extractDmlTableName(String sql) {

        CurdParser curdParser = CurdParserFactory.createCurdParser(sql);

        Curd parse = curdParser.parse();
        if (parse instanceof Update) {
            return (String) ((Update) parse).getTableName().getLiteral();
        } else if (parse instanceof Insert) {
            return (String) ((Insert) parse).getTableName().getLiteral();
        } else if (parse instanceof Delete) {
            return (String) ((Delete) parse).getTableName().getLiteral();
        }

        return null;
    }


    /**
     * @Author ISJINHAO
     * @Date 2022/1/26 18:03
     */
    private static class CurdAddComparisonConditionVisitor implements CurdVisitor<Curd> {

        // 被添加的 comparison
        private final Comparison comparison;

        // 允许被添加 comparison 字段的表名
        private final Set<String> availableTableNameSet;

        private final boolean allTableNameAvailableFg;

        public CurdAddComparisonConditionVisitor(Comparison comparison, Set<String> availableTableNameSet) {
            this.comparison = comparison;
            this.availableTableNameSet = availableTableNameSet;
            allTableNameAvailableFg = false;
        }

        public CurdAddComparisonConditionVisitor(Comparison comparison) {
            this.comparison = comparison;
            this.availableTableNameSet = null;
            this.allTableNameAvailableFg = true;
        }

        @Override
        public Curd visitSelect(Select select) {
            select.getLeftCurd().accept(this);

            Curd rightCurd = select.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return select;
        }

        @Override
        public Curd visitSingleSelect(SingleSelect singleSelect) {

            Curd tableSeg = singleSelect.getTableSeg();
            tableSeg.accept(this);
            AstMetaData astMetaData = tableSeg.getAstMetaData();
            Set<String> tableNameSets = astMetaData.getAvailableTableNameSet();

            WhereSeg whereSeg = (WhereSeg) singleSelect.getWhereSeg();
            // 对于不存在where条件的语法，修改singleSelect的whereSeg属性的值
            if (whereSeg == null) {
                Set<String> availableViewName = getAvailableViewName(astMetaData);
                Curd logicForAdding = createLogic(tableNameSets, availableViewName);
                if (logicForAdding != null) {
                    whereSeg = new WhereSeg(logicForAdding);
                    ReflectUtils.setFieldValue(singleSelect, "whereSeg", whereSeg);
                }
            }
            // 对于存在where条件的语法，修改whereSeg的logic属性的值
            else {
                Curd logic = whereSeg.getLogic();
                whereSeg.accept(this);
                Set<String> availableViewName = getAvailableViewName(astMetaData);
                Curd logicForAdding = createLogic(tableNameSets, availableViewName);
                if (logicForAdding != null) {
                    logicForAdding = new Logic(logic, new Token(TokenType.AND, "and"), logicForAdding);
                    ReflectUtils.setFieldValue(whereSeg, "logic", logicForAdding);
                }
            }
            return singleSelect;
        }


        /**
         * 传入的availableTableNameSet是表名，但是在SQL中会使用别名，所以这一步是将表名转为别名
         */
        private Set<String> getAvailableViewName(AstMetaData astMetaData) {
            return availableTableNameSet == null ? new HashSet<>() :
                availableTableNameSet.stream().map(item -> {
                        String viewAliasName = astMetaData.getViewAliasName(new Identifier(new Token(TokenType.IDENTIFIER, item)));
                        if (viewAliasName == null) {
                            return item;
                        }
                        return viewAliasName;
                    }
                ).collect(Collectors.toSet());
        }

        /**
         * @param tableNameSets SQL里面解析出来的表名，不会为空
         */
        private Curd createLogic(Set<String> tableNameSets, Set<String> availableTableNameSet) {
            Curd result = null;
            for (String tableName : tableNameSets) {
                if (result == null && (allTableNameAvailableFg || availableTableNameSet.contains(tableName))) {
                    Comparison deepClone = (Comparison) comparison.deepClone();
                    Curd leftCurd = deepClone.getLeftCurd();
                    leftCurd.fillTableName(tableName);
                    result = deepClone;
                } else if ((allTableNameAvailableFg || availableTableNameSet.contains(tableName))) {
                    Comparison deepClone = (Comparison) comparison.deepClone();
                    Curd leftCurd = deepClone.getLeftCurd();
                    leftCurd.fillTableName(tableName);
                    result = new Logic(result, new Token(TokenType.AND, "and"), deepClone);
                }
            }
            return result;
        }

        @Override
        public Curd visitColumnSeg(ColumnSeg columnSeg) {
            return columnSeg;
        }

        @Override
        public Curd visitColumnRep(ColumnRep columnRep) {
            return columnRep;
        }

        @Override
        public Curd visitTableSeg(TableSeg tableSeg) {
            tableSeg.getLeftCurd().accept(this);
            Curd rightCurd = tableSeg.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return tableSeg;
        }

        @Override
        public Curd visitTableRep(TableRep tableRep) {
            Curd curd = tableRep.getCurd();
            curd.accept(this);
            return tableRep;
        }

        @Override
        public Curd visitInCondition(InCondition inCondition) {
            Curd curd = inCondition.getCurd();
            if (curd != null) {
                curd.accept(this);
            }
            return inCondition;
        }

        @Override
        public Curd visitExistsCondition(ExistsCondition existsCondition) {
            Curd curd = existsCondition.getCurd();
            curd.accept(this);
            return existsCondition;
        }

        @Override
        public Curd visitGroupBySeg(GroupBySeg groupBySeg) {
            return groupBySeg;
        }

        @Override
        public Curd visitOrderBySeg(OrderBySeg orderBySeg) {
            return orderBySeg;
        }

        @Override
        public Curd visitLimitSeg(LimitSeg limitSeg) {
            return limitSeg;
        }

        @Override
        public Curd visitGroupFunction(GroupFunction groupFunction) {
            return groupFunction;
        }

        @Override
        public Curd visitCaseWhen(CaseWhen caseWhen) {
            return caseWhen;
        }

        @Override
        public Curd visitInsert(Insert insert) {
            insert.getCurd().accept(this);
            return null;
        }

        @Override
        public Curd visitInsertValuesRep(InsertValuesRep insertValuesRep) {
            return null;
        }

        @Override
        public Curd visitInsertSetRep(InsertSetRep insertSetRep) {
            return null;
        }

        @Override
        public Curd visitOnDuplicateKey(OnDuplicateKey onDuplicateKey) {
            return null;
        }

        @Override
        public Curd visitInsertSelectRep(InsertSelectRep insertSelectRep) {
            Curd select = insertSelectRep.getSelect();
            select.accept(this);
            return null;
        }

        @Override
        public Curd visitUpdate(Update update) {
            Curd whereSeg = update.getWhereSeg();
            if (whereSeg != null) {
                whereSeg.accept(this);
            }
            return null;
        }

        @Override
        public Curd visitDelete(Delete delete) {
            Curd whereSeg = delete.getWhereSeg();
            if (whereSeg != null) {
                whereSeg.accept(this);
            }
            return null;
        }

        @Override
        public Curd visitWhereSeg(WhereSeg whereSeg) {
            whereSeg.getLogic().accept(this);
            return whereSeg;
        }

        @Override
        public Curd visitLogic(Logic logic) {
            logic.getLeftCurd().accept(this);

            Curd rightCurd = logic.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return logic;
        }

        @Override
        public Curd visitComparison(Comparison comparison) {
            comparison.getLeftCurd().accept(this);

            Curd rightCurd = comparison.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return comparison;
        }

        @Override
        public Curd visitBinaryArithmetic(BinaryArithmetic binaryArithmetic) {
            binaryArithmetic.getLeftCurd().accept(this);
            Curd rightCurd = binaryArithmetic.getRightCurd();
            if (rightCurd != null) {
                rightCurd.accept(this);
            }
            return binaryArithmetic;
        }

        @Override
        public Curd visitUnaryArithmetic(UnaryArithmetic unaryArithmetic) {
            unaryArithmetic.getCurd().accept(this);
            return unaryArithmetic;
        }

        @Override
        public Curd visitLiteral(Literal literal) {
            return literal;
        }

        @Override
        public Curd visitGrouping(Grouping grouping) {
            grouping.getCurd().accept(this);
            return grouping;
        }

        @Override
        public Curd visitIdentifier(Identifier identifier) {
            return identifier;
        }

        @Override
        public Curd visitFunction(Function function) {
            return function;
        }

        @Override
        public Curd visitAssignmentList(AssignmentList assignmentList) {
            return assignmentList;
        }

        @Override
        public Curd visitTimeInterval(TimeInterval timeInterval) {
            return timeInterval;
        }

        @Override
        public Curd visitTimeUnit(TimeUnit timeUnit) {
            return timeUnit;
        }

        @Override
        public Curd visitIsNot(IsNot isNot) {
            return isNot;
        }

    }


}
