package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;

import java.util.ArrayList;
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
public class IdentifierExistsVisitor extends AbstractIdentifierVisitor {

    private final List<String> identifierTableNameList;

    private final List<String> unIdentifierTableNameList;

    private final boolean reportAmbiguous;

    private String ambiguousInfo;

    private boolean exists = false;

    public IdentifierExistsVisitor(List<String> identifierTableNameList, List<String> unIdentifierTableNameList, String identifier, boolean reportAmbiguous) {
        super(identifier);
        this.identifierTableNameList = identifierTableNameList;
        this.unIdentifierTableNameList = unIdentifierTableNameList;
        this.reportAmbiguous = reportAmbiguous;
    }

    public IdentifierExistsVisitor(List<String> identifierTableNameList, String identifier) {
        super(identifier);
        this.identifierTableNameList = identifierTableNameList;
        this.unIdentifierTableNameList = null;
        this.reportAmbiguous = false;
    }

    public IdentifierExistsVisitor(String identifier) {
        super(identifier);
        this.identifierTableNameList = null;
        this.unIdentifierTableNameList = null;
        this.reportAmbiguous = false;
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
                        break;
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
                        break;
                    }
                    // 如果没有表存在字段，则表示不是availableList里的表
                    else {
                        // no-op
                    }
                } else {
                    String tableName = viewToTableMap.get(owner);
                    if (tableName != null && JdbcSQLUtils.contains(tableName, identifierTableNameList, unIdentifierTableNameList)) {
                        exists = true;
                        break;
                    }
                }
            }
        }

        log.debug("SQLObject: [{}], viewToTableMap: [{}], identifierList: [{}], exists: [{}].", DruidSQLUtils.toLowerCaseSQL(x), viewToTableMap, identifierList, exists);
        super.endVisit(x);
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

