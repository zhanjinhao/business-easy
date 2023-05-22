package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

import cn.addenda.businesseasy.jdbc.DruidSQLUtils;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.visitor.addcondition.TableAddJoinConditionVisitor;
import cn.addenda.businesseasy.jdbc.visitor.additem.InsertAddItemVisitor;
import cn.addenda.businesseasy.jdbc.visitor.additem.InsertSelectAddItemMode;
import cn.addenda.businesseasy.jdbc.visitor.additem.UpdateItemMode;
import cn.addenda.businesseasy.jdbc.visitor.identifier.IdentifierExistsVisitor;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;

import java.util.List;
import java.util.function.Consumer;

import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import lombok.extern.slf4j.Slf4j;

/**
 * @author addenda
 * @since 2023/4/30 19:42
 */
@Slf4j
public class DruidTombstoneSqlRewriter extends AbstractTombstoneSqlRewriter {

    public DruidTombstoneSqlRewriter(List<String> included, List<String> notIncluded) {
        super(included, notIncluded);
        if (included == null) {
            log.warn("未声明逻辑删除的表集合，所有的表都会进行逻辑删除改写！");
        }
    }

    public DruidTombstoneSqlRewriter() {
        this(null, null);
    }

    public String rewriteInsertSql(String sql, boolean useSubQuery) {
        return DruidSQLUtils.statementMerge(sql, s -> doRewriteInsertSql(s, useSubQuery));
    }

    /**
     * insert语句增加item
     */
    private String doRewriteInsertSql(SQLStatement sqlStatement, boolean useSubQuery) {
        doRewriteSql(sqlStatement, sql -> {
            // insert into A(..., if_del) values(..., 0)

            // InsertSelectAddItemMode.ITEM：新增的数据if_del都为0，所以用ITEM模式

            // false的原因如下：
            // 在物理删除的场景下，如果冲突，此条数据业务上一定是存在的。
            // 在逻辑删除的场景下，如果冲突，此条数据业务上可能存在也可能不存在
            // - 业务上不存在：执行duplicateKeyUpdate，if_del = 0，会让此条数据存在。与真实业务背离
            // - 业务上存在：执行duplicateKeyUpdate，if_del = 0，不会有任何影响

            //  UpdateItemMode.ALL：固定为0，不可能为空
            new InsertAddItemVisitor((MySqlInsertStatement) sqlStatement, included, notIncluded, TOMBSTONE_ITEM, true,
                    InsertSelectAddItemMode.ITEM, false, UpdateItemMode.ALL).visit();
            // 处理 insert A (...) select ... from B
            sql.accept(new TableAddJoinConditionVisitor(included, notIncluded, NON_TOMBSTONE, useSubQuery, true));
        });
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    @Override
    public String rewriteDeleteSql(String sql) {
        return DruidSQLUtils.statementMerge(sql, this::doRewriteDeleteSql);
    }

    private String doRewriteDeleteSql(SQLStatement sqlStatement) {
        doRewriteSql(sqlStatement, sql -> {
            // delete from A where ... and if_del = 0
            // false: delete 语句是单表
            sql.accept(new TableAddJoinConditionVisitor(included, notIncluded, NON_TOMBSTONE, false, true));
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
    public String rewriteSelectSql(String sql, boolean useSubQuery) {
        return DruidSQLUtils.statementMerge(sql, s -> doRewriteSelectSql(s, useSubQuery));
    }

    /**
     * select增加where条件condition字段
     */
    private String doRewriteSelectSql(SQLStatement sqlStatement, boolean useSubQuery) {
        doRewriteSql(sqlStatement, sql -> {
            // select a from A where ... and if_del = 0
            sql.accept(new TableAddJoinConditionVisitor(included, notIncluded, NON_TOMBSTONE, useSubQuery, true));
        });
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    @Override
    public String rewriteUpdateSql(String sql) {
        return DruidSQLUtils.statementMerge(sql, this::doRewriteUpdateSql);
    }

    /**
     * update语句where增加condition
     */
    private String doRewriteUpdateSql(SQLStatement sqlStatement) {
        doRewriteSql(sqlStatement, sql -> {
            // update A set ... where ... and if_del = 0
            sql.accept(new TableAddJoinConditionVisitor(included, notIncluded, NON_TOMBSTONE, false, true));
        });
        return DruidSQLUtils.toLowerCaseSQL(sqlStatement);
    }

    private void doRewriteSql(SQLStatement sqlStatement, Consumer<SQLStatement> consumer) {
        IdentifierExistsVisitor identifierExistsVisitor = new IdentifierExistsVisitor(
                sqlStatement, TOMBSTONE_NAME, included, notIncluded, false);
        if (identifierExistsVisitor.isExists()) {
            String msg = String.format("使用逻辑删除的表不能使用[%s]字段，SQL：[%s]。", TOMBSTONE_NAME, DruidSQLUtils.toLowerCaseSQL(sqlStatement));
            throw new TombstoneException(msg);
        }
        consumer.accept(sqlStatement);
    }

}
