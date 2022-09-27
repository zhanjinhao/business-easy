package cn.addenda.businesseasy.cdc.sql;

import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.expression.visitor.ExpressionVisitor;

/**
 * @author addenda
 * @datetime 2022/9/25 11:08
 */
public class CalculableColumnVisitor extends ExpressionVisitor<Boolean> {

    private CalculableColumnVisitor() {
    }

    private static final CalculableColumnVisitor instance = new CalculableColumnVisitor();

    public static CalculableColumnVisitor getInstance() {
        return instance;
    }

    @Override
    public Boolean visitInCondition(InCondition inCondition) {
        return false;
    }

    @Override
    public Boolean visitWhereSeg(WhereSeg whereSeg) {
        return false;
    }

    @Override
    public Boolean visitLogic(Logic logic) {
        return false;
    }

    @Override
    public Boolean visitComparison(Comparison comparison) {
        return false;
    }

    @Override
    public Boolean visitBinaryArithmetic(BinaryArithmetic binaryArithmetic) {
        return false;
    }

    @Override
    public Boolean visitUnaryArithmetic(UnaryArithmetic unaryArithmetic) {
        return false;
    }

    @Override
    public Boolean visitLiteral(Literal literal) {
        return false;
    }

    @Override
    public Boolean visitGrouping(Grouping grouping) {
        return false;
    }

    @Override
    public Boolean visitIdentifier(Identifier identifier) {
        return false;
    }

    @Override
    public Boolean visitFunction(Function function) {
        return false;
    }

    @Override
    public Boolean visitAssignmentList(AssignmentList assignmentList) {
        return false;
    }

    @Override
    public Boolean visitTimeInterval(TimeInterval timeInterval) {
        return false;
    }

    @Override
    public Boolean visitTimeUnit(TimeUnit timeUnit) {
        return false;
    }

    @Override
    public Boolean visitIsNot(IsNot isNot) {
        return false;
    }
}
