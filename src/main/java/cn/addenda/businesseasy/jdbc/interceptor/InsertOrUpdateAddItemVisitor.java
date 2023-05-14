package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.util.BEArrayUtils;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement.ValuesClause;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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

    public InsertOrUpdateAddItemVisitor(String tableName, Item item) {
        this.included = tableName == null ? null : BEArrayUtils.asArrayList(tableName);
        this.notIncluded = null;
        this.item = item;
        this.itemName = item.getItemName();
        this.itemValue = item.getItemValue();
    }

    public InsertOrUpdateAddItemVisitor(List<String> included, List<String> notIncluded, Item item) {
        this.included = included;
        this.notIncluded = notIncluded;
        this.item = item;
        this.itemName = item.getItemName();
        this.itemValue = item.getItemValue();
    }

    @Override
    public void endVisit(MySqlUpdateStatement x) {
        Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(x.getTableSource());

        List<SQLUpdateSetItem> items = x.getItems();

        // 如果 itemName 存在了
        for (SQLUpdateSetItem sqlUpdateSetItem : items) {
            SQLExpr column = sqlUpdateSetItem.getColumn();
            if (DruidSQLUtils.toLowerCaseSQL(column).equalsIgnoreCase(itemName)) {
                String msg = String.format("SQLObject：[%s]种已经存在[%s]，无法新增！", DruidSQLUtils.toLowerCaseSQL(x), itemName);
                throw new JdbcException(msg);
            }
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

        // 如果 itemName 存在了
        for (SQLExpr column : columns) {
            if (DruidSQLUtils.toLowerCaseSQL(column).equalsIgnoreCase(itemName)) {
                String msg = String.format("SQLObject：[%s]种已经存在[%s]，无法新增！", DruidSQLUtils.toLowerCaseSQL(x), itemName);
                throw new JdbcException(msg);
            }
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
            SQLSelectItem sqlSelectItem = new SQLSelectItem();
            sqlSelectItem.setAlias(itemName);
            sqlSelectItem.setExpr(DruidSQLUtils.objectToSQLExpr(0));
            log.debug("SQLObject: [{}], 增加 itemValue：[{}]。", DruidSQLUtils.toLowerCaseSQL(query), sqlSelectItem);
            mySqlSelectQueryBlock.addSelectItem(sqlSelectItem);
        } else if (query instanceof SQLUnionQuery) {
            SQLUnionQuery sqlUnionQuery = (SQLUnionQuery) query;
            List<SQLSelectQuery> relations = sqlUnionQuery.getRelations();
            for (SQLSelectQuery relation : relations) {
                sqlSelectQueryAddSelectItem(relation);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InsertOrUpdateAddItemVisitor that = (InsertOrUpdateAddItemVisitor) o;
        return Objects.equals(included, that.included) &&
                Objects.equals(notIncluded, that.notIncluded) && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(included, notIncluded, item);
    }

    @Override
    public String toString() {
        return "InsertOrUpdateAddItemVisitor{" +
                "included=" + included +
                ", notIncluded=" + notIncluded +
                ", item=" + item +
                "} " + super.toString();
    }
}
