package cn.addenda.businesseasy.fieldfilling.sql;

import cn.addenda.ro.grammar.ast.AstMetaData;
import cn.addenda.ro.grammar.ast.CurdVisitor;
import cn.addenda.ro.grammar.ast.create.*;
import cn.addenda.ro.grammar.ast.delete.Delete;
import cn.addenda.ro.grammar.ast.retrieve.*;
import cn.addenda.ro.grammar.ast.statement.*;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;
import cn.addenda.ro.grammar.util.ReflectUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author ISJINHAO
 * @Date 2022/1/26 18:03
 */
public class CurdAddComparisonConditionVisitor implements CurdVisitor<Curd> {

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
     *
     * @param astMetaData
     * @return
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
     * @return
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
