package cn.addenda.businesseasy.cdc.sql;

import cn.addenda.ro.grammar.ast.CurdVisitor;
import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.expression.visitor.ExpressionVisitorForDelegation;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.util.List;

/**
 * @author addenda
 * @datetime 2022/9/4 14:03
 */
public class BinaryArithmeticPlainValueVisitor extends ExpressionVisitorForDelegation<Boolean> {
    protected BinaryArithmeticPlainValueVisitor(CurdVisitor<Boolean> client) {
        super(client);
    }

    private static final BinaryArithmeticPlainValueVisitor instance = new BinaryArithmeticPlainValueVisitor(null);

    public static BinaryArithmeticPlainValueVisitor getInstance() {
        return instance;
    }

    @Override
    public Boolean visitWhereSeg(WhereSeg whereSeg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitLogic(Logic logic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitComparison(Comparison comparison) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitBinaryArithmetic(BinaryArithmetic binaryArithmetic) {
        Boolean left = binaryArithmetic.getLeftCurd().accept(this);
        if (Boolean.FALSE.equals(left)) {
            return false;
        }
        Curd rightCurd = binaryArithmetic.getRightCurd();
        if (rightCurd != null) {
            return Boolean.TRUE.equals(rightCurd.accept(this));
        }
        return true;
    }

    @Override
    public Boolean visitUnaryArithmetic(UnaryArithmetic unaryArithmetic) {
        return unaryArithmetic.getCurd().accept(this);
    }

    @Override
    public Boolean visitLiteral(Literal literal) {
        return true;
    }

    @Override
    public Boolean visitGrouping(Grouping grouping) {
        return grouping.getCurd().accept(this);
    }

    @Override
    public Boolean visitIdentifier(Identifier identifier) {
        return false;
    }

    @Override
    public Boolean visitFunction(Function function) {
        List<Curd> parameterList = function.getParameterList();
        if (parameterList == null) {
            return true;
        }
        for (Curd curd : parameterList) {
            if (Boolean.FALSE.equals(curd.accept(this))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Boolean visitAssignmentList(AssignmentList assignmentList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitTimeInterval(TimeInterval timeInterval) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitTimeUnit(TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitIsNot(IsNot isNot) {
        throw new UnsupportedOperationException();
    }
}
