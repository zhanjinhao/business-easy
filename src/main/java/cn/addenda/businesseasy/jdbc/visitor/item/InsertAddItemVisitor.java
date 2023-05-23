package cn.addenda.businesseasy.jdbc.visitor.item;

import cn.addenda.businesseasy.jdbc.DruidSQLUtils;
import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.visitor.ViewToTableVisitor;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement.ValuesClause;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author addenda
 * @since 2023/5/10 17:50
 */
@Slf4j
public class InsertAddItemVisitor extends AbstractAddItemVisitor<MySqlInsertStatement, List<Item>> {

    private final Item item;
    private final String itemName;
    private final Object itemValue;
    private final boolean reportItemNameExists;
    private final InsertSelectAddItemMode insertSelectAddItemMode;
    private final boolean duplicateKeyUpdate;
    private final UpdateItemMode updateItemMode;

    public InsertAddItemVisitor(String sql, Item item) {
        this(sql, null, null, item, false,
                InsertSelectAddItemMode.ITEM, false, UpdateItemMode.NOT_NULL);
    }

    public InsertAddItemVisitor(String sql, List<String> included, List<String> notIncluded, Item item,
                                boolean reportItemNameExists, InsertSelectAddItemMode insertSelectAddItemMode,
                                boolean duplicateKeyUpdate, UpdateItemMode updateItemMode) {
        super(sql, included, notIncluded);
        this.item = item;
        this.itemName = item.getItemName();
        this.itemValue = item.getItemValue();
        this.reportItemNameExists = reportItemNameExists;
        this.insertSelectAddItemMode = insertSelectAddItemMode;
        this.duplicateKeyUpdate = duplicateKeyUpdate;
        this.updateItemMode = updateItemMode;
    }

    public InsertAddItemVisitor(MySqlInsertStatement sql, Item item) {
        this(sql, null, null, item, false,
                InsertSelectAddItemMode.ITEM, false, UpdateItemMode.NOT_NULL);
    }

    public InsertAddItemVisitor(MySqlInsertStatement sql, List<String> included, List<String> notIncluded, Item item,
                                boolean reportItemNameExists, InsertSelectAddItemMode insertSelectAddItemMode,
                                boolean duplicateKeyUpdate, UpdateItemMode updateItemMode) {
        super(sql, included, notIncluded);
        this.item = item;
        this.itemName = item.getItemName();
        this.itemValue = item.getItemValue();
        this.reportItemNameExists = reportItemNameExists;
        this.insertSelectAddItemMode = insertSelectAddItemMode;
        this.duplicateKeyUpdate = duplicateKeyUpdate;
        this.updateItemMode = updateItemMode;
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

        if (checkItemNameExists(x, columns, itemName, reportItemNameExists)) {
            return;
        }

        log.debug("SQLObject: [{}], 增加 itemName：[{}]。", DruidSQLUtils.toLowerCaseSQL(x), itemName);
        columns.add(SQLUtils.toSQLExpr(itemName));

        List<ValuesClause> valuesList = x.getValuesList();
        if (valuesList != null && !valuesList.isEmpty()) {
            for (ValuesClause valuesClause : valuesList) {
                log.debug("SQLObject: [{}], 增加 itemValue：[{}]。", DruidSQLUtils.toLowerCaseSQL(x), itemValue);
                valuesClause.addValue(DruidSQLUtils.objectToSQLExpr(itemValue));
            }
            if (duplicateKeyUpdate) {
                List<SQLExpr> duplicateKeyUpdateList = x.getDuplicateKeyUpdate();
                if (UpdateItemMode.ALL == updateItemMode) {
                    duplicateKeyUpdateList.add(newItemBinaryOpExpr(itemName, itemValue));
                } else if (UpdateItemMode.NOT_NULL == updateItemMode) {
                    if (itemValue != null) {
                        duplicateKeyUpdateList.add(newItemBinaryOpExpr(itemName, itemValue));
                    }
                } else if (UpdateItemMode.NOT_EMPTY == updateItemMode) {
                    if (itemValue instanceof CharSequence && !JdbcSQLUtils.isEmpty((CharSequence) itemValue)) {
                        duplicateKeyUpdateList.add(newItemBinaryOpExpr(itemName, itemValue));
                    }
                }
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
            if (insertSelectAddItemMode == InsertSelectAddItemMode.DB || insertSelectAddItemMode == InsertSelectAddItemMode.DB_FIRST) {
                // 优先从数据库取数
                Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(mySqlSelectQueryBlock.getFrom());
                if (viewToTableMap.size() > 1) {
                    if (insertSelectAddItemMode == InsertSelectAddItemMode.DB) {
                        String msg = String.format("无法从SQL中推断出来需要增加的itemName，SQL：[%s]，item：[%s]。",
                                DruidSQLUtils.toLowerCaseSQL(query), item);
                        throw new JdbcException(msg);
                    } else if (insertSelectAddItemMode == InsertSelectAddItemMode.DB_FIRST) {
                        addItemFromItem(mySqlSelectQueryBlock);
                    }
                }

                // 单表场景下，也会存在无result的场景。
                // eg: select 1 from  (  select a  from dual  d1 join dual  d2 on d1.id  = d2.outer_id    )  t1
                String resultItemName = addItemFromDb(viewToTableMap, mySqlSelectQueryBlock);
                if (resultItemName == null) {
                    // 如果从数据库取不到数据
                    if (insertSelectAddItemMode == InsertSelectAddItemMode.DB) {
                        String msg = String.format("SQL无法增加itemName，SQL：[%s]，item：[%s]。",
                                DruidSQLUtils.toLowerCaseSQL(query), item);
                        throw new JdbcException(msg);
                    } else if (insertSelectAddItemMode == InsertSelectAddItemMode.DB_FIRST) {
                        addItemFromItem(mySqlSelectQueryBlock);
                    }
                }

            } else if (insertSelectAddItemMode == InsertSelectAddItemMode.ITEM) {
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
        SelectAddItemVisitor visitor = new SelectAddItemVisitor(
                sqlSelectStatement, included, notIncluded, view, itemName, false);
        visitor.visit();

        return visitor.getResult();
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

    @Override
    public String toString() {
        return "InsertAddItemVisitor{" +
                "item=" + item +
                ", reportItemNameExists=" + reportItemNameExists +
                ", insertSelectAddItemMode=" + insertSelectAddItemMode +
                ", duplicateKeyUpdate=" + duplicateKeyUpdate +
                ", updateItemMode=" + updateItemMode +
                "} " + super.toString();
    }
}
