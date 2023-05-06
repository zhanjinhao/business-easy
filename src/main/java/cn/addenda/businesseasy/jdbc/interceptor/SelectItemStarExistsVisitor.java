package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;

/**
 * @author addenda
 * @since 2023/5/6 20:33
 */
public class SelectItemStarExistsVisitor extends SQLASTVisitorAdapter {

    private final boolean visitAggregateFunction;

    private boolean exists = false;

    public SelectItemStarExistsVisitor(boolean visitAggregateFunction) {
        this.visitAggregateFunction = visitAggregateFunction;
    }

    public SelectItemStarExistsVisitor() {
        this.visitAggregateFunction = true;
    }

    @Override
    public void endVisit(SQLPropertyExpr x) {
        if ("*".equals(x.getName())) {
            exists = true;
        }
    }

    @Override
    public void endVisit(SQLAllColumnExpr x) {
        exists = true;
    }

    @Override
    public boolean visit(SQLAggregateExpr x) {
        return visitAggregateFunction;
    }

    public boolean isExists() {
        return exists;
    }

}
