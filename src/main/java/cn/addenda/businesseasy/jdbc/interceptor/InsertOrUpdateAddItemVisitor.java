package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.util.BEArrayUtils;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement.ValuesClause;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @since 2023/5/10 17:50
 */
@Slf4j
public class InsertOrUpdateAddItemVisitor extends MySqlASTVisitorAdapter {

    private final List<String> included;
    private final List<String> notIncluded;
    private final Item item;
    private final String itemName;
    private final Object itemValue;
    private final boolean reportItemNameExists;
    private final AddItemMode mode;

    public InsertOrUpdateAddItemVisitor(Item item) {
        this(null, null, item, false, AddItemMode.ITEM);
    }

    public InsertOrUpdateAddItemVisitor(String tableName, Item item, boolean reportItemNameExists) {
        this(tableName == null ? null : BEArrayUtils.asArrayList(tableName),
                null, item, reportItemNameExists, AddItemMode.ITEM);
    }

    public InsertOrUpdateAddItemVisitor(List<String> included, List<String> notIncluded, Item item, boolean reportItemNameExists, AddItemMode mode) {
        this.included = included;
        this.notIncluded = notIncluded;
        this.item = item;
        this.itemName = item.getItemName();
        this.itemValue = item.getItemValue();
        this.reportItemNameExists = reportItemNameExists;
        this.mode = mode;
    }

    @Override
    public void endVisit(MySqlUpdateStatement x) {
        Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(x.getTableSource());

        List<SQLUpdateSetItem> items = x.getItems();

        List<SQLExpr> columns = items.stream().map(SQLUpdateSetItem::getColumn).collect(Collectors.toList());
        if (!checkItemNameExists(x, columns)) {
            return;
        }

        boolean prefix = viewToTableMap.size() != 1;
        viewToTableMap.forEach((view, table) -> {
            if (table != null && JdbcSQLUtils.include(table, included, notIncluded)) {
                SQLUpdateSetItem sqlUpdateSetItem = new SQLUpdateSetItem();
                SQLExpr sqlExpr;
                if (prefix) {
                    sqlExpr = SQLUtils.toSQLExpr(view + "." + itemName);
                } else {
                    sqlExpr = SQLUtils.toSQLExpr(itemName);
                }
                sqlUpdateSetItem.setColumn(sqlExpr);
                sqlUpdateSetItem.setValue(DruidSQLUtils.objectToSQLExpr(itemValue));
                log.debug("SQLObject: [{}], 增加 item：[{}]。", DruidSQLUtils.toLowerCaseSQL(x), sqlUpdateSetItem);
                items.add(sqlUpdateSetItem);
            }
        });
    }


    @Override
    public void endVisit(MySqlInsertStatement x) {
        Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(x.getTableSource());
        if (viewToTableMap.size() != 1) {
            String msg = String.format("insert 语句增加item仅支持单表，SQL: [%s]。", DruidSQLUtils.toLowerCaseSQL(x));
            throw new JdbcException(msg);
        }
        String table = null;
        for (Entry<String, String> stringEntry : viewToTableMap.entrySet()) {
            table = stringEntry.getValue();
        }

        if (table == null) {
            String msg = String.format("找不到表名，SQL: [%s]。", DruidSQLUtils.toLowerCaseSQL(x));
            throw new JdbcException(msg);
        }

        if (!JdbcSQLUtils.include(table, included, notIncluded)) {
            return;
        }

        List<SQLExpr> columns = x.getColumns();

        if (!checkItemNameExists(x, columns)) {
            return;
        }

        log.debug("SQLObject: [{}], 增加 itemName：[{}]。", DruidSQLUtils.toLowerCaseSQL(x), itemName);
        columns.add(SQLUtils.toSQLExpr(itemName));

        List<ValuesClause> valuesList = x.getValuesList();
        if (valuesList != null) {
            for (ValuesClause valuesClause : valuesList) {
                log.debug("SQLObject: [{}], 增加 itemValue：[{}]。", DruidSQLUtils.toLowerCaseSQL(x), itemValue);
                valuesClause.addValue(DruidSQLUtils.objectToSQLExpr(itemValue));
            }
        }

        SQLSelect sqlSelect = x.getQuery();
        if (sqlSelect != null) {
            // sqlSelect的返回值可能已经存在 itemName了，再增加一个也无问题
            sqlSelectQueryAddSelectItem(sqlSelect.getQuery());
        }
    }

    private void sqlSelectQueryAddSelectItem(SQLSelectQuery query) {
        if (query instanceof MySqlSelectQueryBlock) {
            MySqlSelectQueryBlock mySqlSelectQueryBlock = (MySqlSelectQueryBlock) query;
            if (mode == AddItemMode.DB || mode == AddItemMode.DB_FIRST) {
                // 优先从数据库取数
                Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(mySqlSelectQueryBlock.getFrom());
                if (viewToTableMap.size() > 1) {
                    if (mode == AddItemMode.DB) {
                        String msg = String.format("无法从SQL中推断出来需要增加的itemName，SQL：[%s]，item：[%s]。",
                                DruidSQLUtils.toLowerCaseSQL(query), item);
                        throw new JdbcException(msg);
                    } else if (mode == AddItemMode.DB_FIRST) {
                        addItemFromItem(mySqlSelectQueryBlock);
                    }
                }

                // 单表场景下，也会存在无resultItem的场景。
                // eg: select 1 from  (  select a  from dual  d1 join dual  d2 on d1.id  = d2.outer_id    )  t1
                String resultItemName = addItemFromDb(viewToTableMap, mySqlSelectQueryBlock);
                if (resultItemName == null) {
                    // 如果从数据库取不到数据
                    if (mode == AddItemMode.DB) {
                        String msg = String.format("SQL无法增加itemName，SQL：[%s]，item：[%s]。",
                                DruidSQLUtils.toLowerCaseSQL(query), item);
                        throw new JdbcException(msg);
                    } else if (mode == AddItemMode.DB_FIRST) {
                        addItemFromItem(mySqlSelectQueryBlock);
                    }
                }

            } else if (mode == AddItemMode.ITEM) {
                addItemFromItem(mySqlSelectQueryBlock);
            }
        } else if (query instanceof SQLUnionQuery) {
            SQLUnionQuery sqlUnionQuery = (SQLUnionQuery) query;
            List<SQLSelectQuery> relations = sqlUnionQuery.getRelations();
            for (SQLSelectQuery relation : relations) {
                sqlSelectQueryAddSelectItem(relation);
            }
        }
    }

    private void addItemFromItem(MySqlSelectQueryBlock mySqlSelectQueryBlock) {
        SQLSelectItem sqlSelectItem = new SQLSelectItem();
        sqlSelectItem.setAlias(itemName);
        sqlSelectItem.setExpr(DruidSQLUtils.objectToSQLExpr(0));
        mySqlSelectQueryBlock.addSelectItem(sqlSelectItem);
        log.debug("SQLObject: [{}], 增加 item：[{}]。", DruidSQLUtils.toLowerCaseSQL(mySqlSelectQueryBlock), itemName);
    }

    /**
     * @param viewToTableMap size()确定是1
     * @return 数据库的数据为1
     */
    private String addItemFromDb(Map<String, String> viewToTableMap, MySqlSelectQueryBlock mySqlSelectQueryBlock) {
        String view = null;
        for (Entry<String, String> entry : viewToTableMap.entrySet()) {
            view = entry.getKey();
        }
        SQLSelectStatement sqlSelectStatement = wrapSQLSelectQuery(mySqlSelectQueryBlock);
        SelectResultAddItemNameVisitor visitor = new SelectResultAddItemNameVisitor(
                sqlSelectStatement, included, notIncluded, view, itemName);
        visitor.visit();

        return visitor.getResultItemName();
    }

    private SQLSelectStatement wrapSQLSelectQuery(SQLSelectQuery query) {
        SQLSelectStatement sqlSelectStatement = new SQLSelectStatement();
        SQLSelect sqlSelect = new SQLSelect();
        sqlSelectStatement.setSelect(sqlSelect);
        sqlSelect.setParent(sqlSelectStatement);
        sqlSelect.setQuery(query);
        query.setParent(sqlSelect);
        return sqlSelectStatement;
    }

    /**
     * @return 是否注入item
     */
    private boolean checkItemNameExists(SQLObject x, List<SQLExpr> columnList) {
        for (SQLExpr column : columnList) {
            if (DruidSQLUtils.toLowerCaseSQL(column).equalsIgnoreCase(itemName)) {
                if (reportItemNameExists) {
                    String msg = String.format("SQLObject：[%s]种已经存在[%s]，无法新增！", DruidSQLUtils.toLowerCaseSQL(x), itemName);
                    throw new JdbcException(msg);
                } else {
                    // 不能新增，否则SQL执行时会报错
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "InsertOrUpdateAddItemVisitor{" +
                "included=" + included +
                ", notIncluded=" + notIncluded +
                ", item=" + item +
                "} " + super.toString();
    }


    public enum AddItemMode {
        ITEM,
        DB,
        DB_FIRST,
    }

}
