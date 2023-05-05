package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class IdentifierExistsVisitor extends SQLASTVisitorAdapter {

    private List<String> avaliableList;

    private String identifier;

    private final Deque<List<String>> identifierListStack = new ArrayDeque<>();

    private boolean exists = false;

    public IdentifierExistsVisitor(List<String> avaliableList, String identifier) {
        this.avaliableList = avaliableList;
        this.identifier = identifier;

    }

    public IdentifierExistsVisitor(String identifier) {
        this.avaliableList = null;
        this.identifier = identifier;
    }

    @Override
    public void endVisit(SQLPropertyExpr x) {
        // x是a.b结构，
        // 对于表：dbName.tableName；对于字段：owner.column
        // 只需要比较b和identifier是否一致即可。
        // 如果表名正好等于identifier，也会加入集合
        if (x.getName().equalsIgnoreCase(identifier)) {
            List<String> identifierList = identifierListStack.peek();
            identifierList.add(x.toString());
        }
    }

    @Override
    public void endVisit(SQLIdentifierExpr x) {
        if (x.getName().equalsIgnoreCase(identifier)) {
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
    public void endVisit(SQLSelectQueryBlock x) {
        Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(x.getFrom());
        List<String> identifierList = identifierListStack.peek();
        log.debug("SQLObject: [{}], viewToTableMap: [{}], identifierList: [{}], exists: [{}].", DruidSQLUtils.toLowerCaseSQL(x, false), viewToTableMap, identifierList, exists);

        if (exists) {
            // 如果已经存在，就直接返回了
        } else {
            for (String _identifier : identifierList) {
                String owner = JdbcSQLUtils.extractColumnOwner(_identifier);
                if (owner == null) {
                    List<String> declaredTableList = new ArrayList<>();
                    viewToTableMap.forEach((view, table) -> {
                        if (table != null && JdbcSQLUtils.contains(table, avaliableList, null)) {
                            declaredTableList.add(table);
                        }
                    });

                    // 如果只有一个表存在字段，则identifier存在
                    if (declaredTableList.size() == 1) {
                        exists = true;
                    }
                    // 如果多个表存在字段，则抛出异常
                    else if (declaredTableList.size() > 1) {
                        log.debug("SQLObject: [{}], Ambiguous identifier: [{}], declaredTableList: [{}].", DruidSQLUtils.toLowerCaseSQL(x, false), identifier, declaredTableList);
                        throw new JdbcException("SQLObject: [" + DruidSQLUtils.toLowerCaseSQL(x, false) + "], Ambiguous identifier: [" + identifier + "], declaredTableList: [" + declaredTableList + "].");
                    }
                    // 如果没有表存在字段，则表示不是avaliableList里的表
                    else {
                        // no-op
                    }
                } else {
                    String tableName = viewToTableMap.get(owner);
                    if (tableName != null && JdbcSQLUtils.contains(tableName, avaliableList, null)) {
                        exists = true;
                    }
                }
            }
        }
        identifierListStack.pop();
    }

    @Override
    public boolean visit(SQLSelectQueryBlock x) {
        identifierListStack.push(new ArrayList<>());
        return true;
    }

    public boolean isExists() {
        return exists;
    }

}

