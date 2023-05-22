package cn.addenda.businesseasy.jdbc.visitor.additem;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.DruidSQLUtils;
import cn.addenda.businesseasy.jdbc.visitor.ViewToTableVisitor;
import cn.addenda.businesseasy.util.BEArrayUtils;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.ast.statement.SQLUnionQueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLValuesQuery;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * @author addenda
 * @since 2023/5/1 13:28
 */
@Slf4j
public class SelectAddItemVisitor extends AbstractAddItemVisitor<SQLSelectStatement, String> {

    private static final String ITEM_KEY = "BaseEntitySelectItemList";

    private String masterView;

    private final boolean reportAmbiguous;

    private final String itemName;

    private String ambiguousInfo;

    public SelectAddItemVisitor(String sql, List<String> included, List<String> notIncluded,
                                String masterView, String itemName, boolean reportAmbiguous) {
        super(sql, included, notIncluded);
        this.masterView = masterView;
        this.itemName = itemName;
        this.reportAmbiguous = reportAmbiguous;
    }

    public SelectAddItemVisitor(String sql, String masterView, String itemName) {
        this(sql, null, null, masterView, itemName, false);
    }

    public SelectAddItemVisitor(
            SQLSelectStatement sqlSelectStatement, List<String> included, List<String> notIncluded,
            String masterView, String itemName, boolean reportAmbiguous) {
        super(sqlSelectStatement, included, notIncluded);
        this.masterView = masterView;
        this.itemName = itemName;
        this.reportAmbiguous = reportAmbiguous;
    }

    public SelectAddItemVisitor(
            SQLSelectStatement sqlSelectStatement, String masterView, String itemName) {
        this(sqlSelectStatement, null, null, masterView, itemName, false);
    }

    @Override
    public SQLSelectStatement visitAndOutputAst() {
        sqlStatement.accept(ViewToTableVisitor.getInstance());
        sqlStatement.accept(this);
        return sqlStatement;
    }

    private int depth = 0;

    @Override
    public void endVisit(SQLSelectGroupByClause x) {
        List<SQLExpr> items = x.getItems();

        // items 里面存在的基础列才能被注入到返回值
        List<SQLExpr> injectedList = new ArrayList<>();
        for (SQLExpr sqlExpr : items) {
            if (JdbcSQLUtils.include(JdbcSQLUtils.extractColumnName(sqlExpr.toString()), BEArrayUtils.asArrayList(itemName), null)) {
                injectedList.add(sqlExpr);
            }
        }

        if (injectedList.isEmpty()) {
            return;
        }

        Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(x.getParent());
        List<SelectResultSelectItem> selectResultSelectItemList = new ArrayList<>();
        for (SQLExpr sqlExpr : injectedList) {
            String owner = JdbcSQLUtils.extractColumnOwner(sqlExpr.toString());
            if (owner == null) {
                List<String> declaredTableList = new ArrayList<>();
                viewToTableMap.forEach((view, tableName) -> {
                    if (tableName != null && JdbcSQLUtils.include(tableName, included, notIncluded)) {
                        declaredTableList.add(tableName);
                    }
                });

                if (declaredTableList.size() == 1) {
                    String view = declaredTableList.get(0);
                    selectResultSelectItemList.add(new SelectResultSelectItem(SQLUtils.toSQLExpr(view + "." + sqlExpr), view + "_" + sqlExpr));
                } else if (declaredTableList.size() > 1) {
                    ambiguousInfo = String.format("SQLObject: [%s], Ambiguous identifier: [%s], declaredTableList: [%s].",
                            DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(sqlExpr), declaredTableList);
                    selectResultSelectItemList.add(new SelectResultSelectItem(sqlExpr, sqlExpr.toString()));
                    if (reportAmbiguous) {
                        throw new JdbcException(ambiguousInfo);
                    } else {
                        log.warn(ambiguousInfo);
                    }
                } else {
                    // no-op
                }

            } else {
                String tableName = viewToTableMap.get(owner);
                if (tableName != null && JdbcSQLUtils.include(tableName, included, notIncluded)) {
                    selectResultSelectItemList.add(new SelectResultSelectItem(sqlExpr, sqlExpr.toString().replace(".", "_")));
                }
            }
        }
        putItemList(x, selectResultSelectItemList);
    }

    @Override
    public void endVisit(SQLExprTableSource x) {
        String alias = x.getAlias();
        String tableName = x.getTableName();
        String view = alias == null ? tableName : alias;
        if (!JdbcSQLUtils.include(tableName, included, notIncluded)) {
            return;
        }
        List<SelectResultSelectItem> selectResultSelectItemList = new ArrayList<>();
        selectResultSelectItemList.add(
                new SelectResultSelectItem(SQLUtils.toSQLExpr(view + "." + itemName), view + "_" + itemName));
        putItemList(x, selectResultSelectItemList);

        addMasterTableName(x, tableName);
        addMasterAlias(x, alias);
    }

    @Override
    public void endVisit(SQLSubqueryTableSource x) {
        SQLSelect select = x.getSelect();
        String alias = x.getAlias();
        List<SelectResultSelectItem> selectResultSelectItemList = getItemList(select);
        if (selectResultSelectItemList != null) {
            List<SelectResultSelectItem> xSelectResultSelectItemList = new ArrayList<>();
            for (SelectResultSelectItem item : selectResultSelectItemList) {
                xSelectResultSelectItemList.add(new SelectResultSelectItem(
                        SQLUtils.toSQLExpr(alias + "." + item.getAlias()), alias + "_" + item.getAlias()));
            }
            putItemList(x, xSelectResultSelectItemList);
        }
    }

    @Override
    public void endVisit(SQLJoinTableSource x) {
        inherit(x);

        SQLTableSource left = x.getLeft();
        SQLTableSource right = x.getRight();

        List<SelectResultSelectItem> selectResultSelectItemList = new ArrayList<>();
        List<SelectResultSelectItem> leftSelectResultSelectItemList = getItemList(left);
        if (leftSelectResultSelectItemList != null) {
            selectResultSelectItemList.addAll(leftSelectResultSelectItemList);
        }
        List<SelectResultSelectItem> rightSelectResultSelectItemList = getItemList(right);
        if (rightSelectResultSelectItemList != null) {
            selectResultSelectItemList.addAll(rightSelectResultSelectItemList);
        }
        putItemList(x, selectResultSelectItemList);
    }

    @Override
    public void endVisit(SQLUnionQueryTableSource x) {
        SQLUnionQuery union = x.getUnion();
        String alias = x.getAlias();
        List<SelectResultSelectItem> selectResultSelectItemList = getItemList(union);
        List<SelectResultSelectItem> xSelectResultSelectItemList = new ArrayList<>();
        for (SelectResultSelectItem item : selectResultSelectItemList) {
            xSelectResultSelectItemList.add(new SelectResultSelectItem(
                    SQLUtils.toSQLExpr(alias + "." + item.getAlias()), alias + "_" + item.getAlias()));
        }
        putItemList(x, xSelectResultSelectItemList);
    }

    @Override
    public void endVisit(SQLUnionQuery x) {
        List<SQLSelectQuery> relations = x.getRelations();
        List<SelectResultSelectItem> list = getItemList(relations.get(0));
        boolean flag = true;
        for (int i = 1; i < relations.size(); i++) {
            SQLSelectQuery relation = relations.get(i);
            List<SelectResultSelectItem> relationSelectResultSelectItemList = getItemList(relation);
            if (relationSelectResultSelectItemList == null) {
                flag = false;
                break;
            }
            if (list.size() != relationSelectResultSelectItemList.size()) {
                flag = false;
                break;
            }
            for (int j = 0; j < list.size(); j++) {
                SelectResultSelectItem o1 = list.get(j);
                SelectResultSelectItem o2 = relationSelectResultSelectItemList.get(j);
                if (!o1.equals(o2)) {
                    flag = false;
                    break;
                }
            }
            if (!flag) {
                break;
            }
        }
        if (flag) {
            putItemList(x, new ArrayList<>(list));
        } else {
            clear(x);
        }
    }

    private void clear(SQLSelectQuery sqlSelectQuery) {
        if (sqlSelectQuery instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) sqlSelectQuery;
            List<SQLSelectItem> selectList = sqlSelectQueryBlock.getSelectList();
            selectList.removeIf(SelectResultSelectItem.class::isInstance);
        } else if (sqlSelectQuery instanceof SQLUnionQuery) {
            SQLUnionQuery sqlUnionQuery = (SQLUnionQuery) sqlSelectQuery;
            List<SQLSelectQuery> relations = sqlUnionQuery.getRelations();
            for (SQLSelectQuery relation : relations) {
                clear(relation);
            }
        }
    }

    @Override
    public void endVisit(SQLSelectQueryBlock x) {
        SQLTableSource from = x.getFrom();
        SQLSelectGroupByClause groupBy = x.getGroupBy();
        List<SQLSelectItem> selectList = x.getSelectList();
        // todo *和A.* 场景处理
        Set<SQLExpr> selectExprSet = selectList.stream().map(SQLSelectItem::getExpr).collect(Collectors.toSet());

        List<SelectResultSelectItem> selectResultSelectItemList;
        if (groupBy != null) {
            selectResultSelectItemList = getItemList(groupBy);
        } else {
            selectResultSelectItemList = getItemList(from);
        }
        if (selectResultSelectItemList != null) {
            putItemList(x, new ArrayList<>(selectResultSelectItemList));
            List<String> debugInfo = new ArrayList<>();
            for (SelectResultSelectItem selectResultSelectItem : selectResultSelectItemList) {
                SQLExpr expr = selectResultSelectItem.getExpr();
                if (!selectExprSet.contains(expr)) {
                    debugInfo.add(DruidSQLUtils.toLowerCaseSQL(selectResultSelectItem));
                    x.addSelectItem(selectResultSelectItem);
                }
            }
            log.debug("SQLObject: [{}], 注入列：[{}].", DruidSQLUtils.toLowerCaseSQL(x), debugInfo);
        }

        if (depth == 1) {
            boolean flag = false;
            if (masterView == null) {
                flag = true;
                if (from instanceof SQLExprTableSource) {
                    SQLExprTableSource sqlExprTableSource = (SQLExprTableSource) from;
                    String tableName = sqlExprTableSource.getTableName();
                    String alias = sqlExprTableSource.getAlias();
                    masterView = alias == null ? tableName : alias;
                } else if (from instanceof SQLSubqueryTableSource) {
                    SQLSubqueryTableSource sqlSubqueryTableSource = (SQLSubqueryTableSource) from;
                    masterView = sqlSubqueryTableSource.getAlias();
                } else if (from instanceof SQLUnionQueryTableSource) {
                    SQLUnionQueryTableSource sqlUnionQueryTableSource = (SQLUnionQueryTableSource) from;
                    masterView = sqlUnionQueryTableSource.getAlias();
                } else if (from instanceof SQLJoinTableSource) {
                    String masterTableName = getMasterTableName(from);
                    String masterAlias = getMasterAlias(from);
                    if (masterTableName != null) {
                        masterView = masterAlias == null ? masterTableName : masterAlias;
                    }
                }
            }

            if (masterView == null) {
                // no-op
            } else {
                // 获取到masterView下注入的字段
                List<SQLSelectItem> injected = selectList.stream()
                        .filter(SelectResultSelectItem.class::isInstance)
                        .filter(f -> f.getAlias().toLowerCase().startsWith(masterView.toLowerCase() + "_"))
                        .filter(f -> f.getAlias().endsWith(itemName)).collect(Collectors.toList());

                // 只有当masterView注入的字段个数为1时才进行masterView改写
                if (injected.size() == 1) {
                    injected.get(0).setAlias(itemName);
                    setResult(itemName);
                }
            }

            if (flag) {
                masterView = null;
            }
        }
    }

    @Override
    public boolean visit(SQLSelect x) {
        depth++;
        return true;
    }

    @Override
    public void endVisit(SQLSelect select) {
        SQLSelectQuery query = select.getQuery();
        List<SelectResultSelectItem> itemList = getItemList(query);
        if (itemList != null) {
            putItemList(select, new ArrayList<>(itemList));
        }
        depth--;
    }

    private static final String MASTER_TABLE_NAME_KEY = "MASTER_TABLE_NAME_KEY";
    private static final String MASTER_ALIAS_KEY = "MASTER_ALIAS_KEY";

    protected String getMasterTableName(SQLObject sqlObject) {
        return (String) sqlObject.getAttribute(MASTER_TABLE_NAME_KEY);
    }

    protected String getMasterAlias(SQLObject sqlObject) {
        return (String) sqlObject.getAttribute(MASTER_ALIAS_KEY);
    }

    protected void addMasterTableName(SQLObject sqlObject, String tableName) {
        if (tableName != null) {
            sqlObject.putAttribute(MASTER_TABLE_NAME_KEY, tableName);
        }
    }

    protected void addMasterAlias(SQLObject sqlObject, String alias) {
        if (alias != null) {
            sqlObject.putAttribute(MASTER_ALIAS_KEY, alias);
        }
    }

    private void inherit(SQLObject x, SQLTableSource tableSource) {
        String whereRightTableName = getMasterTableName(tableSource);
        String whereRightAlias = getMasterAlias(tableSource);
        if (whereRightTableName != null) {
            addMasterTableName(x, whereRightTableName);
            addMasterAlias(x, whereRightAlias);
        }
    }

    private void inherit(SQLJoinTableSource x) {
        SQLTableSource left = x.getLeft();
        SQLTableSource right = x.getRight();

        SQLJoinTableSource.JoinType joinType = x.getJoinType();
        if (SQLJoinTableSource.JoinType.LEFT_OUTER_JOIN == joinType) {
            inherit(x, left);
        } else if (SQLJoinTableSource.JoinType.RIGHT_OUTER_JOIN == joinType) {
            inherit(x, right);
        }
    }

    @Override
    public void endVisit(SQLValuesQuery x) {
    }

    @Override
    public boolean visit(SQLSelectItem x) {
        return false;
    }

    @Override
    public boolean visit(SQLBinaryOpExpr x) {
        return false;
    }

    @Override
    public boolean visit(SQLExistsExpr x) {
        return false;
    }

    @Override
    public boolean visit(SQLInSubQueryExpr x) {
        return false;
    }

    @Override
    public boolean visit(SQLContainsExpr x) {
        return false;
    }

    @Override
    public boolean visit(SQLBetweenExpr x) {
        return false;
    }

    private List<SelectResultSelectItem> getItemList(SQLObject sqlObject) {
        return (List<SelectResultSelectItem>) sqlObject.getAttribute(ITEM_KEY);
    }

    private void putItemList(SQLObject sqlObject, List<SelectResultSelectItem> selectResultSelectItemList) {
        if (selectResultSelectItemList != null && !selectResultSelectItemList.isEmpty()) {
            sqlObject.putAttribute(ITEM_KEY, selectResultSelectItemList);
        }
    }

    public String getAmbiguousInfo() {
        return ambiguousInfo;
    }

    public static class SelectResultSelectItem extends SQLSelectItem {

        public SelectResultSelectItem() {
        }

        public SelectResultSelectItem(SQLExpr expr) {
            super(expr);
        }

        public SelectResultSelectItem(int value) {
            super(value);
        }

        public SelectResultSelectItem(SQLExpr expr, String alias) {
            super(expr, alias);
        }

        public SelectResultSelectItem(SQLExpr expr, String alias, boolean connectByRoot) {
            super(expr, alias, connectByRoot);
        }

        public SelectResultSelectItem(SQLExpr expr, List<String> aliasList, boolean connectByRoot) {
            super(expr, aliasList, connectByRoot);
        }

    }

    @Override
    public String toString() {
        return "SelectAddItemVisitor{" +
                "masterView='" + masterView + '\'' +
                ", reportAmbiguous=" + reportAmbiguous +
                ", itemName='" + itemName + '\'' +
                "} " + super.toString();
    }

}
