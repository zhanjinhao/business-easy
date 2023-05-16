package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.interceptor.*;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author addenda
 * @since 2023/5/2 19:35
 */
@Slf4j
public class DruidBaseEntityRewriter extends AbstractBaseEntityRewriter {

    /**
     * 需要基础字段的表
     */
    private final List<String> included;

    /**
     * 不需要基础字段的表
     */
    private final List<String> notIncluded;

    private final boolean hideBaseEntity;

    private InsertOrUpdateAddItemVisitor.AddItemMode addItemMode;

    private static final List<String> INSERT_COLUMN_NAME_LIST;
    private static final List<String> INSERT_FIELD_NAME_LIST;
    private static final List<String> UPDATE_COLUMN_NAME_LIST;
    private static final List<String> UPDATE_FIELD_NAME_LIST;
    private final List<InsertOrUpdateAddItemVisitor> insertVisitorList = new ArrayList<>();
    private final List<InsertOrUpdateAddItemVisitor> updateVisitorList = new ArrayList<>();

    static {
        INSERT_COLUMN_NAME_LIST = BaseEntity.getAllColumnNameList();
        INSERT_FIELD_NAME_LIST = BaseEntity.getAllFieldNameList();
        UPDATE_COLUMN_NAME_LIST = BaseEntity.getUpdateColumnNameList();
        UPDATE_FIELD_NAME_LIST = BaseEntity.getUpdateFieldNameList();

    }

    public DruidBaseEntityRewriter(List<String> included, List<String> notIncluded, BaseEntitySource baseEntitySource, boolean hideBaseEntity, InsertOrUpdateAddItemVisitor.AddItemMode addItemMode) {
        super(baseEntitySource);
        this.included = included;
        this.notIncluded = notIncluded;
        this.hideBaseEntity = hideBaseEntity;
        this.addItemMode = addItemMode;
        if (included == null) {
            log.warn("未声明需填充的基础字段的表集合，所有的表都会进行基础字段填充改写！");
        }

        for (int i = 0; i < UPDATE_COLUMN_NAME_LIST.size(); i++) {
            String columnName = UPDATE_COLUMN_NAME_LIST.get(i);
            String fieldName = UPDATE_FIELD_NAME_LIST.get(i);
            Item item = new Item(columnName, baseEntitySource.get(fieldName));
            updateVisitorList.add(new InsertOrUpdateAddItemVisitor(included, notIncluded, item, hideBaseEntity, addItemMode));
        }
        for (int i = 0; i < INSERT_COLUMN_NAME_LIST.size(); i++) {
            String columnName = INSERT_COLUMN_NAME_LIST.get(i);
            String fieldName = INSERT_FIELD_NAME_LIST.get(i);
            Item item = new Item(columnName, baseEntitySource.get(fieldName));
            insertVisitorList.add(new InsertOrUpdateAddItemVisitor(included, notIncluded, item, hideBaseEntity, addItemMode));
        }
    }

    public DruidBaseEntityRewriter() {
        this(null, null, new DefaultBaseEntitySource(), false, InsertOrUpdateAddItemVisitor.AddItemMode.ITEM);
    }

    @Override
    public String rewriteInsertSql(String sql) {
        return DruidSQLUtils.statementMerge(sql, this::doRewriteInsertSql);
    }

    private String doRewriteInsertSql(SQLStatement sqlStatement) {
        sqlStatement.accept(new ViewToTableVisitor());
        for (InsertOrUpdateAddItemVisitor insertOrUpdateAddItemVisitor : insertVisitorList) {
            sqlStatement.accept(insertOrUpdateAddItemVisitor);
        }

        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    @Override
    public String rewriteDeleteSql(String sql) {
        return sql;
    }

    @Override
    public String rewriteSelectSql(String sql, String masterView) {
        return DruidSQLUtils.statementMerge(sql,
                sqlStatement -> doRewriteSelectSql(sqlStatement, masterView));
    }

    private String doRewriteSelectSql(SQLStatement sqlStatement, String masterView) {
        for (String column : INSERT_COLUMN_NAME_LIST) {
            new SelectResultAddItemNameVisitor(
                    (SQLSelectStatement) sqlStatement, included, notIncluded, masterView, column).visit();
        }
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    @Override
    public String rewriteUpdateSql(String sql) {
        return DruidSQLUtils.statementMerge(sql, this::doRewriteUpdateSql);
    }

    private String doRewriteUpdateSql(SQLStatement sqlStatement) {
        sqlStatement.accept(new ViewToTableVisitor());
        for (InsertOrUpdateAddItemVisitor insertOrUpdateAddItemVisitor : updateVisitorList) {
            sqlStatement.accept(insertOrUpdateAddItemVisitor);
        }
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }
}
