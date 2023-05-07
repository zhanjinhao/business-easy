package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * @author addenda
 * @since 2023/5/7 11:43
 */
public class AbstractIdentifierVisitor extends SQLASTVisitorAdapter {

    protected final String identifier;

    protected final Deque<List<String>> identifierListStack = new ArrayDeque<>();

    public AbstractIdentifierVisitor(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean visit(SQLPropertyExpr x) {
        return false;
    }

    @Override
    public void endVisit(SQLPropertyExpr x) {
        // x是a.b结构，
        // 对于表：dbName.tableName；对于字段：owner.column
        // 只需要比较b和identifier是否一致即可。
        // 如果表名正好等于identifier，也会加入集合
        if (identifier == null || x.getName().equalsIgnoreCase(identifier)) {
            List<String> identifierList = identifierListStack.peek();
            identifierList.add(x.toString());
        }
    }

    @Override
    public void endVisit(SQLIdentifierExpr x) {
        if (identifier == null || x.getName().equalsIgnoreCase(identifier)) {
            List<String> identifierList = identifierListStack.peek();
            identifierList.add(x.getName());
        }
    }

    @Override
    public void endVisit(SQLExprTableSource x) {
        List<String> identifierList = identifierListStack.peek();

        // 这里有两种场景
        // - dbName.tableName，对应SQLPropertyExpr
        // - tableName，对应SQLIdentifierExpr
        SQLExpr expr = x.getExpr();
        String tableName = expr.toString();
        identifierList.remove(tableName);
    }

    @Override
    public boolean visit(SQLSelectQueryBlock x) {
        identifierListStack.push(new ArrayList<>());
        return true;
    }

    @Override
    public void endVisit(SQLSelectQueryBlock x) {
        identifierListStack.pop();
    }

}