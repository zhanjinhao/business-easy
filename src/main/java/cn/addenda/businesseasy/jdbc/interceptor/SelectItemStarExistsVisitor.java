package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;

/**
 * @author addenda
 * @since 2023/5/6 20:33
 */
public class SelectItemStarExistsVisitor extends SQLASTVisitorAdapter {

    private boolean exists = false;

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

    public boolean isExists() {
        return exists;
    }

}
