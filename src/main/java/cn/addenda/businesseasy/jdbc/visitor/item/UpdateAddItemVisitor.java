package cn.addenda.businesseasy.jdbc.visitor.item;

import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.DruidSQLUtils;
import cn.addenda.businesseasy.jdbc.visitor.ViewToTableVisitor;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @since 2023/5/10 17:50
 */
@Slf4j
public class UpdateAddItemVisitor extends AbstractAddItemVisitor<MySqlUpdateStatement, List<Item>> {

    private final Item item;
    private final String itemName;
    private final Object itemValue;
    private final boolean reportItemNameExists;
    private final UpdateItemMode updateItemMode;

    public UpdateAddItemVisitor(String sql, Item item) {
        this(sql, null, null, item, false, UpdateItemMode.NOT_NULL);
    }

    public UpdateAddItemVisitor(String sql, List<String> included, List<String> notIncluded, Item item,
                                boolean reportItemNameExists, UpdateItemMode updateItemMode) {
        super(sql, included, notIncluded);
        this.item = item;
        this.itemName = item.getItemName();
        this.itemValue = item.getItemValue();
        this.reportItemNameExists = reportItemNameExists;
        this.updateItemMode = updateItemMode;
    }

    public UpdateAddItemVisitor(MySqlUpdateStatement sql, Item item) {
        this(sql, null, null, item, false, UpdateItemMode.NOT_NULL);
    }

    public UpdateAddItemVisitor(MySqlUpdateStatement sql, List<String> included, List<String> notIncluded, Item item,
                                boolean reportItemNameExists, UpdateItemMode updateItemMode) {
        super(sql, included, notIncluded);
        this.item = item;
        this.itemName = item.getItemName();
        this.itemValue = item.getItemValue();
        this.reportItemNameExists = reportItemNameExists;
        this.updateItemMode = updateItemMode;
    }

    @Override
    public void endVisit(MySqlUpdateStatement x) {
        Map<String, String> viewToTableMap = ViewToTableVisitor.getViewToTableMap(x.getTableSource());

        List<SQLUpdateSetItem> sqlUpdateSetItemList = x.getItems();

        List<SQLExpr> columns = sqlUpdateSetItemList.stream().map(SQLUpdateSetItem::getColumn).collect(Collectors.toList());
        if (checkItemNameExists(x, columns, itemName, reportItemNameExists)) {
            return;
        }

        boolean prefix = viewToTableMap.size() != 1;
        List<Item> itemList = new ArrayList<>();
        viewToTableMap.forEach((view, table) -> {
            if (table != null && JdbcSQLUtils.include(table, included, notIncluded)) {
                if (UpdateItemMode.ALL == updateItemMode) {
                    addItem(x, view, prefix, itemList, sqlUpdateSetItemList);
                } else if (UpdateItemMode.NOT_NULL == updateItemMode) {
                    if (itemValue != null) {
                        addItem(x, view, prefix, itemList, sqlUpdateSetItemList);
                    }
                } else if (UpdateItemMode.NOT_EMPTY == updateItemMode) {
                    if (itemValue instanceof CharSequence && !JdbcSQLUtils.isEmpty((CharSequence) itemValue)) {
                        addItem(x, view, prefix, itemList, sqlUpdateSetItemList);
                    }
                }
            }
        });
        if (!itemList.isEmpty()) {
            setResult(itemList);
        }
    }

    private void addItem(SQLObject x, String view, boolean prefix, List<Item> itemList, List<SQLUpdateSetItem> sqlUpdateSetItemList) {
        SQLExpr sqlExpr;
        if (prefix) {
            sqlExpr = SQLUtils.toSQLExpr(view + "." + itemName);
        } else {
            sqlExpr = SQLUtils.toSQLExpr(itemName);
        }
        SQLUpdateSetItem sqlUpdateSetItem = new SQLUpdateSetItem();
        sqlUpdateSetItem.setColumn(sqlExpr);
        sqlUpdateSetItem.setValue(DruidSQLUtils.objectToSQLExpr(itemValue));
        log.debug("SQLObject: [{}], 增加 item：[{}]。", DruidSQLUtils.toLowerCaseSQL(x), sqlUpdateSetItem);

        sqlUpdateSetItemList.add(sqlUpdateSetItem);
        itemList.add(new Item(view + "." + itemName, itemValue));
    }

    @Override
    public String toString() {
        return "UpdateAddItemVisitor{" +
                "item=" + item +
                ", reportItemNameExists=" + reportItemNameExists +
                ", updateItemMode=" + updateItemMode +
                "} " + super.toString();
    }
}
