package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.*;
import cn.addenda.businesseasy.util.BEArrayUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;

import java.util.List;
import java.util.function.Consumer;

import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import lombok.extern.slf4j.Slf4j;

/**
 * @author addenda
 * @since 2023/4/30 19:42
 */
@Slf4j
public class DruidTombstoneSqlRewriter extends AbstractDruidSqlRewriter implements TombstoneSqlRewriter {

    private static final String TOMBSTONE_NAME = "if_del";
    private static final Integer NON_TOMBSTONE_VALUE = 0;
    private static final Integer TOMBSTONE_VALUE = 1;
    private static final String NON_TOMBSTONE = TOMBSTONE_NAME + "=" + NON_TOMBSTONE_VALUE;
    private static final String TOMBSTONE = TOMBSTONE_NAME + "=" + TOMBSTONE_VALUE;

    private final boolean useSubQuery;
    private final boolean rewriteCommaToJoin;

    private final boolean hideTombstone;

    /**
     * 逻辑删除的表
     */
    private final List<String> included;

    /**
     * 非逻辑删除的表
     */
    private final List<String> notIncluded = BEArrayUtils.asArrayList("dual");

    public DruidTombstoneSqlRewriter(List<String> included, boolean useSubQuery, boolean rewriteCommaToJoin, boolean hideTombstone) {
        this.included = included;
        this.useSubQuery = useSubQuery;
        this.rewriteCommaToJoin = rewriteCommaToJoin;
        this.hideTombstone = hideTombstone;
        if (included == null) {
            log.warn("未声明逻辑删除的表集合，所有的表都会进行逻辑删除改写！");
        }
    }

    public DruidTombstoneSqlRewriter() {
        this(null, false, true, false);
    }

    @Override
    public String rewriteInsertSql(String sql) {
        return singleRewriteSql(sql, this::doRewriteInsertSql);
    }

    /**
     * insert语句增加item
     */
    private String doRewriteInsertSql(SQLStatement sqlStatement) {
        doRewriteSql(sqlStatement, sql -> {
            // insert into A(..., if_del) values(..., 0)
            sql.accept(new InsertOrUpdateAddItemVisitor(included, notIncluded, TOMBSTONE_NAME, NON_TOMBSTONE_VALUE));
            // 处理 insert A (...) select ... from B
            sql.accept(new TableAddJoinConditionVisitor(included, notIncluded, NON_TOMBSTONE, useSubQuery, rewriteCommaToJoin));
        });
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    @Override
    public String rewriteDeleteSql(String sql) {
        return singleRewriteSql(sql, this::doRewriteDeleteSql);
    }

    private String doRewriteDeleteSql(SQLStatement sqlStatement) {
        doRewriteSql(sqlStatement, sql -> {
            // delete from A where ... and if_del = 0
            sql.accept(new TableAddJoinConditionVisitor(included, notIncluded, NON_TOMBSTONE, useSubQuery, rewriteCommaToJoin));
        });
        MySqlDeleteStatement mySqlDeleteStatement = (MySqlDeleteStatement) sqlStatement;
        SQLName tableName = mySqlDeleteStatement.getTableName();
        if (!JdbcSQLUtils.include(tableName.toString(), included, notIncluded)) {
            return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
        }
        SQLExpr where = mySqlDeleteStatement.getWhere();
        // update A set if_del = 1 where ... and if_del = 0
        return "update " + mySqlDeleteStatement.getTableName() + " set " + TOMBSTONE + " where " + DruidSQLUtils.toLowerCaseSQL(where);
    }

    @Override
    public String rewriteSelectSql(String sql) {
        return singleRewriteSql(sql, this::doRewriteSelectSql);
    }

    /**
     * select增加where条件condition字段
     */
    private String doRewriteSelectSql(SQLStatement sqlStatement) {
        doRewriteSql(sqlStatement, sql -> {
            // select a from A where ... and if_del = 0
            sql.accept(new TableAddJoinConditionVisitor(included, notIncluded, NON_TOMBSTONE, useSubQuery, rewriteCommaToJoin));
        });
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    @Override
    public String rewriteUpdateSql(String sql) {
        return singleRewriteSql(sql, this::doRewriteUpdateSql);
    }

    /**
     * update语句where增加condition
     */
    private String doRewriteUpdateSql(SQLStatement sqlStatement) {
        doRewriteSql(sqlStatement, sql -> {
            // update A set ... where ... and if_del = 0
            sql.accept(new TableAddJoinConditionVisitor(included, notIncluded, NON_TOMBSTONE, useSubQuery, rewriteCommaToJoin));
        });
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    private void doRewriteSql(SQLStatement sqlStatement, Consumer<SQLStatement> consumer) {
        IdentifierExistsVisitor identifierExistsVisitor = null;
        boolean exists = false;
        // 开启隐藏tombstone，则sql里不能存在 if_del
        if (hideTombstone) {
            identifierExistsVisitor = new IdentifierExistsVisitor(
                    sqlStatement, TOMBSTONE_NAME, included, notIncluded, false);
        }
        // 不开启隐藏tombstone
        else {
            // insert 语句需要保证 values 里没有 if_del 字段
            // update 语句需要保证 set 里没有 if_del 字段
            if (sqlStatement instanceof MySqlInsertStatement || sqlStatement instanceof MySqlUpdateStatement) {
                identifierExistsVisitor = new InsertOrUpdateItemNameIdentifierExistsVisitor(
                        sqlStatement, TOMBSTONE_NAME, included, notIncluded, false);
            }
        }

        if (identifierExistsVisitor != null) {
            identifierExistsVisitor.visit();
            exists = identifierExistsVisitor.isExists();
        }
        if (exists) {
            String msg = String.format("使用逻辑删除的表不能使用[%s]字段，SQL：[%s]。", TOMBSTONE_NAME, DruidSQLUtils.toLowerCaseSQL(sqlStatement));
            throw new TombstoneException(msg);
        }
        consumer.accept(sqlStatement);
    }

}
