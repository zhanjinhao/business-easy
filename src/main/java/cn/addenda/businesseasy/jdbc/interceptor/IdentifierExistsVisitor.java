package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 假如有表A、B、C，其中B和C是存在列name的，要检测SQL里面有没有B和C的name列。<br/>
 * 如果表集合为空，检测全部的表。
 *
 * @author addenda
 * @since 2023/5/3 20:55
 */
@Slf4j
public class IdentifierExistsVisitor extends SQLASTVisitorAdapter {

    private final List<String> identifierTableNameList;

    private final List<String> unIdentifierTableNameList;

    private final String identifier;

    private final boolean reportAmbiguous;

    private String ambiguousInfo;

    private boolean exists = false;

    private final Deque<List<String>> identifierListStack = new ArrayDeque<>();

    public IdentifierExistsVisitor(List<String> identifierTableNameList, List<String> unIdentifierTableNameList, String identifier, boolean reportAmbiguous) {
        this.identifierTableNameList = identifierTableNameList;
        this.unIdentifierTableNameList = unIdentifierTableNameList;
        this.identifier = identifier;
        this.reportAmbiguous = reportAmbiguous;
    }

    public IdentifierExistsVisitor(List<String> identifierTableNameList, String identifier) {
        this.identifierTableNameList = identifierTableNameList;
        this.unIdentifierTableNameList = null;
        this.identifier = identifier;
        this.reportAmbiguous = false;
    }

    public IdentifierExistsVisitor(String identifier) {
        this.identifierTableNameList = null;
        this.unIdentifierTableNameList = null;
        this.identifier = identifier;
        this.reportAmbiguous = false;
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

        if (exists) {
            // 如果已经存在，就直接返回了
        } else {
            for (String _identifier : identifierList) {
                String owner = JdbcSQLUtils.extractColumnOwner(_identifier);
                if (owner == null) {
                    List<String> declaredTableList = new ArrayList<>();
                    viewToTableMap.forEach((view, table) -> {
                        if (table != null && JdbcSQLUtils.contains(table, identifierTableNameList, unIdentifierTableNameList)) {
                            declaredTableList.add(table);
                        }
                    });

                    // 如果只有一个表存在字段，则identifier存在
                    if (declaredTableList.size() == 1) {
                        exists = true;
                    }
                    // 如果多个表存在字段，则抛出异常
                    else if (declaredTableList.size() > 1) {
                        ambiguousInfo = "SQLObject: [" + DruidSQLUtils.toLowerCaseSQL(x) + "], Ambiguous identifier: [" + identifier + "], declaredTableList: [" + declaredTableList + "].";
                        exists = true;
                        if (reportAmbiguous) {
                            throw new JdbcException(ambiguousInfo);
                        } else {
                            log.debug(ambiguousInfo);
                        }
                    }
                    // 如果没有表存在字段，则表示不是availableList里的表
                    else {
                        // no-op
                    }
                } else {
                    String tableName = viewToTableMap.get(owner);
                    if (tableName != null && JdbcSQLUtils.contains(tableName, identifierTableNameList, unIdentifierTableNameList)) {
                        exists = true;
                    }
                }
            }
        }

        log.debug("SQLObject: [{}], viewToTableMap: [{}], identifierList: [{}], exists: [{}].", DruidSQLUtils.toLowerCaseSQL(x), viewToTableMap, identifierList, exists);
        identifierListStack.pop();
    }

    @Override
    public boolean visit(SQLSelectQueryBlock x) {
        identifierListStack.push(new ArrayList<>());
        return true;
    }

    @Override
    public void endVisit(SQLSelectStatement x) {
        log.debug("SQLObject: [{}], exists: [{}].", DruidSQLUtils.toLowerCaseSQL(x), exists);
    }

    public boolean isExists() {
        return exists;
    }

    public String getAmbiguousInfo() {
        return ambiguousInfo;
    }

}

