package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * @author addenda
 * @since 2023/5/5 20:50
 */
public class SelectItemIdentifierExistsVisitor extends IdentifierExistsVisitor {

    private final Deque<Boolean> flagStack = new ArrayDeque<>();

    public SelectItemIdentifierExistsVisitor(List<String> included, List<String> notIncluded, String identifier, boolean reportAmbiguous) {
        super(included, notIncluded, identifier, reportAmbiguous);
    }

    public SelectItemIdentifierExistsVisitor(List<String> included, String identifier) {
        super(included, identifier);
    }

    public SelectItemIdentifierExistsVisitor(String identifier) {
        super(identifier);
    }

    @Override
    public void endVisit(SQLSelectItem x) {
        flagStack.pop();
        flagStack.push(false);
        super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSelectItem x) {
        flagStack.pop();
        flagStack.push(true);
        return super.visit(x);
    }


    @Override
    public void endVisit(SQLPropertyExpr x) {
        if (Boolean.FALSE.equals(flagStack.peek())) {
            return;
        }
        super.endVisit(x);
    }

    @Override
    public void endVisit(SQLIdentifierExpr x) {
        if (Boolean.FALSE.equals(flagStack.peek())) {
            return;
        }
        super.endVisit(x);
    }

    @Override
    public void endVisit(SQLSelectQueryBlock x) {
        flagStack.pop();
        super.endVisit(x);
    }

    @Override
    public boolean visit(SQLSelectQueryBlock x) {
        flagStack.push(false);
        super.visit(x);
        return true;
    }

}
