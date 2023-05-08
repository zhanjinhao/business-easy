package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.DruidSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.ViewToTableVisitor;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @since 2023/5/1 13:28
 */
@Slf4j
public class DruidSelectAddBaseEntityVisitor extends MySqlASTVisitorAdapter {

    private static final String ITEM_KEY = "BaseEntitySelectItemList";

    private final List<String> included;
    private final List<String> notIncluded;

    private String masterView;

    private String sql;

    private SQLSelectStatement sqlSelectStatement;

    private final boolean reportAmbiguous;

    private String ambiguousInfo;

    private static final List<String> COLUMN_NAME_LIST;

    static {
        COLUMN_NAME_LIST = BaseEntity.getAllColumnNameList();
    }

    public DruidSelectAddBaseEntityVisitor(
            List<String> included, List<String> notIncluded,
            String masterView, String sql, boolean reportAmbiguous) {
        this.included = included;
        this.notIncluded = notIncluded;
        this.masterView = masterView;
        this.sql = sql;
        this.reportAmbiguous = reportAmbiguous;
        this.init();
    }

    public DruidSelectAddBaseEntityVisitor(
            List<String> included, List<String> notIncluded,
            String masterView, SQLSelectStatement sqlSelectStatement, boolean reportAmbiguous) {
        this.included = included;
        this.notIncluded = notIncluded;
        this.masterView = masterView;
        this.sqlSelectStatement = sqlSelectStatement;
        this.reportAmbiguous = reportAmbiguous;
        this.init();
    }

    public DruidSelectAddBaseEntityVisitor(
            List<String> included, List<String> notIncluded,
            String masterView, SQLSelectStatement sqlSelectStatement) {
        this.included = included;
        this.notIncluded = notIncluded;
        this.masterView = masterView;
        this.sqlSelectStatement = sqlSelectStatement;
        this.reportAmbiguous = false;
        this.init();
    }

    private void init() {
        if (sqlSelectStatement == null && sql == null) {
            throw new NullPointerException("SQL 不存在！");
        }
        if (sql == null) {
            this.sql = DruidSQLUtils.toLowerCaseSQL(sqlSelectStatement);
        }
        if (!JdbcSQLUtils.isSelect(sql)) {
            throw new IllegalArgumentException("请传入select语句，当前SQL：" + sql + "。");
        }
        if (sqlSelectStatement == null) {
            if (!JdbcSQLUtils.isSelect(sql)) {
                throw new IllegalArgumentException("请传入select语句，当前SQL：" + sql + "。");
            }
            List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);
            if (stmtList.size() != 1) {
                throw new IllegalArgumentException("字段填充仅支持单条SQL，当前SQL：" + sql + "。");
            }
            if (stmtList.get(0) instanceof SQLSelectStatement) {
                throw new IllegalArgumentException("字段填充仅支持单条SQL，当前SQL：" + sql + "。");
            }
            this.sqlSelectStatement = (SQLSelectStatement) stmtList.get(0);
        }
        sqlSelectStatement.accept(new ViewToTableVisitor());
    }

    public SQLSelectStatement visit() {
        sqlSelectStatement.accept(this);
        return sqlSelectStatement;
    }

    private int deepth = 0;

    @Override
    public void endVisit(SQLSelectGroupByClause x) {
        List<SQLExpr> items = x.getItems();

        // items 里面存在的基础列才能被注入到返回值
        List<SQLExpr> injectedList = new ArrayList<>();
        for (SQLExpr sqlExpr : items) {
            if (JdbcSQLUtils.include(JdbcSQLUtils.extractColumnName(sqlExpr.toString()), COLUMN_NAME_LIST, null)) {
                injectedList.add(sqlExpr);
            }
        }

        if (injectedList.isEmpty()) {
            return;
        }

        Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(x.getParent());
        List<BaseEntitySelectItem> baseEntitySelectItemList = new ArrayList<>();
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
                    baseEntitySelectItemList.add(new BaseEntitySelectItem(SQLUtils.toSQLExpr(view + "." + sqlExpr), view + "_" + sqlExpr));
                } else if (declaredTableList.size() > 1) {
                    ambiguousInfo = "SQLObject: [" + DruidSQLUtils.toLowerCaseSQL(x) + "], Ambiguous identifier: [" + DruidSQLUtils.toLowerCaseSQL(sqlExpr) + "], declaredTableList: [" + declaredTableList + "].";
                    baseEntitySelectItemList.add(new BaseEntitySelectItem(sqlExpr, sqlExpr.toString()));
                    if (reportAmbiguous) {
                        throw new JdbcException(ambiguousInfo);
                    } else {
                        log.debug(ambiguousInfo);
                    }
                } else {
                    // no-op
                }

            } else {
                String tableName = viewToTableMap.get(owner);
                if (tableName != null && JdbcSQLUtils.include(tableName, included, notIncluded)) {
                    baseEntitySelectItemList.add(new BaseEntitySelectItem(sqlExpr, sqlExpr.toString().replace(".", "_")));
                }
            }
        }

        x.putAttribute(ITEM_KEY, baseEntitySelectItemList);
    }

    @Override
    public void endVisit(SQLExprTableSource x) {
        String alias = x.getAlias();
        String tableName = x.getTableName();
        String view = alias == null ? tableName : alias;
        if (!JdbcSQLUtils.include(tableName, included, notIncluded)) {
            return;
        }
        List<BaseEntitySelectItem> baseEntitySelectItemList = new ArrayList<>();
        for (String item : COLUMN_NAME_LIST) {
            baseEntitySelectItemList.add(
                    new BaseEntitySelectItem(SQLUtils.toSQLExpr(view + "." + item), view + "_" + item));
        }
        x.putAttribute(ITEM_KEY, baseEntitySelectItemList);
    }

    @Override
    public void endVisit(SQLSubqueryTableSource x) {
        SQLSelect select = x.getSelect();
        String alias = x.getAlias();
        List<BaseEntitySelectItem> baseEntitySelectItemList = getItemList(select);
        if (baseEntitySelectItemList != null) {
            List<BaseEntitySelectItem> xBaseEntitySelectItemList = new ArrayList<>();
            for (BaseEntitySelectItem item : baseEntitySelectItemList) {
                xBaseEntitySelectItemList.add(new BaseEntitySelectItem(
                        SQLUtils.toSQLExpr(alias + "." + item.getAlias()), alias + "_" + item.getAlias()));
            }
            x.putAttribute(ITEM_KEY, xBaseEntitySelectItemList);
        }
    }

    @Override
    public void endVisit(SQLJoinTableSource x) {
        SQLTableSource left = x.getLeft();
        SQLTableSource right = x.getRight();
        List<BaseEntitySelectItem> baseEntitySelectItemList = new ArrayList<>();
        List<BaseEntitySelectItem> leftBaseEntitySelectItemList = getItemList(left);
        if (leftBaseEntitySelectItemList != null) {
            baseEntitySelectItemList.addAll(leftBaseEntitySelectItemList);
        }
        List<BaseEntitySelectItem> rightBaseEntitySelectItemList = getItemList(right);
        if (rightBaseEntitySelectItemList != null) {
            baseEntitySelectItemList.addAll(rightBaseEntitySelectItemList);
        }
        x.putAttribute(ITEM_KEY, baseEntitySelectItemList);
    }


    @Override
    public void endVisit(SQLUnionQueryTableSource x) {
        SQLUnionQuery union = x.getUnion();
        String alias = x.getAlias();
        List<BaseEntitySelectItem> baseEntitySelectItemList = getItemList(union);
        List<BaseEntitySelectItem> xBaseEntitySelectItemList = new ArrayList<>();
        for (BaseEntitySelectItem item : baseEntitySelectItemList) {
            xBaseEntitySelectItemList.add(new BaseEntitySelectItem(
                    SQLUtils.toSQLExpr(alias + "." + item.getAlias()), alias + "_" + item.getAlias()));
        }
        x.putAttribute(ITEM_KEY, xBaseEntitySelectItemList);

    }

    @Override
    public void endVisit(SQLUnionQuery x) {
        List<SQLSelectQuery> relations = x.getRelations();
        List<BaseEntitySelectItem> list = getItemList(relations.get(0));
        boolean flag = true;
        for (int i = 1; i < relations.size(); i++) {
            SQLSelectQuery relation = relations.get(i);
            List<BaseEntitySelectItem> relationBaseEntitySelectItemList = getItemList(relation);
            if (relationBaseEntitySelectItemList == null) {
                flag = false;
                break;
            }
            if (list.size() != relationBaseEntitySelectItemList.size()) {
                flag = false;
                break;
            }
            for (int j = 0; j < list.size(); j++) {
                BaseEntitySelectItem o1 = list.get(j);
                BaseEntitySelectItem o2 = relationBaseEntitySelectItemList.get(j);
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
            x.putAttribute(ITEM_KEY, new ArrayList<>(list));
        } else {
            clear(x);
        }
    }

    private void clear(SQLSelectQuery sqlSelectQuery) {
        if (sqlSelectQuery instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) sqlSelectQuery;
            List<SQLSelectItem> selectList = sqlSelectQueryBlock.getSelectList();
            selectList.removeIf(BaseEntitySelectItem.class::isInstance);
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
        Set<SQLExpr> selectExprSet = selectList.stream().map(SQLSelectItem::getExpr).collect(Collectors.toSet());

        List<BaseEntitySelectItem> baseEntitySelectItemList;
        if (groupBy != null) {
            baseEntitySelectItemList = getItemList(groupBy);
        } else {
            baseEntitySelectItemList = getItemList(from);
        }
        if (baseEntitySelectItemList != null) {
            x.putAttribute(ITEM_KEY, new ArrayList<>(baseEntitySelectItemList));
            List<String> debugInfo = new ArrayList<>();
            for (BaseEntitySelectItem baseEntitySelectItem : baseEntitySelectItemList) {
                SQLExpr expr = baseEntitySelectItem.getExpr();
                if (!selectExprSet.contains(expr)) {
                    debugInfo.add(DruidSQLUtils.toLowerCaseSQL(baseEntitySelectItem));
                    x.addSelectItem(baseEntitySelectItem);
                }
            }
            log.debug("SQLObject: [{}], 注入列：[{}].", DruidSQLUtils.toLowerCaseSQL(x), debugInfo);
        }


        if (deepth == 1) {
            boolean flag = false;
            if (masterView == null) {
                flag = true;
                if (from instanceof SQLExprTableSource) {
                    SQLExprTableSource sqlExprTableSource = (SQLExprTableSource) from;
                    String tableName = sqlExprTableSource.getTableName();
                    String alias = sqlExprTableSource.getAlias();
                    masterView = tableName == null ? alias : tableName;
                } else if (from instanceof SQLSubqueryTableSource) {
                    SQLSubqueryTableSource sqlSubqueryTableSource = (SQLSubqueryTableSource) from;
                    masterView = sqlSubqueryTableSource.getAlias();
                } else if (from instanceof SQLUnionQueryTableSource) {
                    SQLUnionQueryTableSource sqlUnionQueryTableSource = (SQLUnionQueryTableSource) from;
                    masterView = sqlUnionQueryTableSource.getAlias();
                }
            }


            if (masterView == null) {
                // no-op
            } else {
                for (SQLSelectItem selectItem : selectList) {
                    if (!(selectItem instanceof BaseEntitySelectItem)) {
                        continue;
                    }
                    String alias = selectItem.getAlias();
                    if (!alias.toLowerCase().startsWith(masterView.toLowerCase() + "_")) {
                        continue;
                    }
                    for (String columnName : COLUMN_NAME_LIST) {
                        if (alias.endsWith(columnName)) {
                            selectItem.setAlias(columnName);
                        }
                    }
                }
            }

            if (flag) {
                masterView = null;
            }
        }
    }

    @Override
    public boolean visit(SQLSelect x) {
        deepth++;
        return true;
    }

    @Override
    public void endVisit(SQLSelect select) {
        SQLSelectQuery query = select.getQuery();
        List<BaseEntitySelectItem> itemList = getItemList(query);
        if (itemList != null) {
            select.putAttribute(ITEM_KEY, new ArrayList<>(itemList));
        }
        deepth--;
    }

    @Override
    public void endVisit(SQLValuesQuery x) {
    }

    private List<BaseEntitySelectItem> getItemList(SQLObject sqlObject) {
        return (List<BaseEntitySelectItem>) sqlObject.getAttribute(ITEM_KEY);
    }

    public String getAmbiguousInfo() {
        return ambiguousInfo;
    }

    private class BaseEntitySelectItem extends SQLSelectItem {

        public BaseEntitySelectItem() {
        }

        public BaseEntitySelectItem(SQLExpr expr) {
            super(expr);
        }

        public BaseEntitySelectItem(int value) {
            super(value);
        }

        public BaseEntitySelectItem(SQLExpr expr, String alias) {
            super(expr, alias);
        }

        public BaseEntitySelectItem(SQLExpr expr, String alias, boolean connectByRoot) {
            super(expr, alias, connectByRoot);
        }

        public BaseEntitySelectItem(SQLExpr expr, List<String> aliasList, boolean connectByRoot) {
            super(expr, aliasList, connectByRoot);
        }

    }

}
