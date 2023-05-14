package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;

/**
 * @author addenda
 * @since 2023/5/6 20:33
 */
public class SelectItemStarExistsVisitor extends SQLBoundVisitor<SQLSelectStatement> {

    private final boolean visitAggregateFunction;

    private boolean exists = false;

    public SelectItemStarExistsVisitor(String sql, boolean visitAggregateFunction) {
        super(sql);
        this.visitAggregateFunction = visitAggregateFunction;
    }

    public SelectItemStarExistsVisitor(String sql) {
        this(sql, true);
    }

    public SelectItemStarExistsVisitor(SQLSelectStatement sql, boolean visitAggregateFunction) {
        super(sql);
        this.visitAggregateFunction = visitAggregateFunction;
    }

    public SelectItemStarExistsVisitor(SQLSelectStatement sql) {
        this(sql, true);
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

    @Override
    public SQLSelectStatement visitAndOutputAst() {
        sqlStatement.accept(this);
        return sqlStatement;
    }

    @Override
    public String toString() {
        return "SelectItemStarExistsVisitor{" +
                "visitAggregateFunction=" + visitAggregateFunction +
                "} " + super.toString();
    }
}
