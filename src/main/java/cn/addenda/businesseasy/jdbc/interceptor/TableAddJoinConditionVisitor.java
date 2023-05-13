package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.util.BEArrayUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource.JoinType;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 先应用过滤条件，再连接
 *
 * @author addenda
 * @since 2023/4/28 10:05
 */
@Slf4j
public class TableAddJoinConditionVisitor extends AbstractAddConditionVisitor {

    private final boolean useSubQuery;

    private final boolean rewriteCommaToJoin;

    public TableAddJoinConditionVisitor(String condition) {
        super(null, null, condition);
        this.useSubQuery = false;
        this.rewriteCommaToJoin = true;
    }

    public TableAddJoinConditionVisitor(String tableName, String condition) {
        super(tableName == null ? null : BEArrayUtils.asArrayList(tableName), null, condition);
        this.useSubQuery = false;
        this.rewriteCommaToJoin = true;
    }

    public TableAddJoinConditionVisitor(String tableName, String condition, boolean useSubQuery) {
        super(tableName == null ? null : BEArrayUtils.asArrayList(tableName), null, condition);
        this.useSubQuery = useSubQuery;
        this.rewriteCommaToJoin = true;
    }

    public TableAddJoinConditionVisitor(
            List<String> included, List<String> notIncluded, String condition, boolean useSubQuery, boolean rewriteCommaToJoin) {
        super(included, notIncluded, condition);
        this.useSubQuery = useSubQuery;
        this.rewriteCommaToJoin = rewriteCommaToJoin;
    }

    @Override
    public void endVisit(SQLJoinTableSource x) {
        SQLTableSource left = x.getLeft();
        String leftTableName = getTableName(left);
        String leftAlias = getAlias(left);
        JoinType joinType = x.getJoinType();

        if (joinType == JoinType.COMMA && rewriteCommaToJoin) {
            // A,B -> 改写为 A JOIN B
            x.setJoinType(JoinType.JOIN);
            joinType = JoinType.JOIN;
        }

        if (leftTableName != null) {
            // A, B 场景下，只能使用子表，不能添加join条件
            if (useSubQuery || JoinType.COMMA == joinType) {
                SQLTableSource tableSource = newFrom(leftTableName, leftAlias);
                log.debug("SQLObject: [{}]，旧的tableSource：[{}]，新的tableSource：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getLeft()), DruidSQLUtils.toLowerCaseSQL(tableSource));
                x.setLeft(tableSource);
            } else {
                SQLExpr condition = newWhere(x.getCondition(), leftTableName, leftAlias);
                log.debug("SQLObject: [{}]，旧的join condition：[{}]，新的join condition：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getCondition()), DruidSQLUtils.toLowerCaseSQL(condition));
                x.setCondition(condition);
            }
            clear(left);
        }

        SQLTableSource right = x.getRight();
        String rightTableName = getTableName(right);
        String rightAlias = getAlias(right);
        if (rightTableName != null) {
            // A, B 场景下，只能使用子表，不能添加join条件
            if (useSubQuery || JoinType.COMMA == joinType) {
                SQLTableSource tableSource = newFrom(rightTableName, rightAlias);
                log.debug("SQLObject: [{}]，旧的tableSource：[{}]，新的tableSource：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getRight()), DruidSQLUtils.toLowerCaseSQL(tableSource));
                x.setRight(tableSource);
            } else {
                SQLExpr condition = newWhere(x.getCondition(), rightTableName, rightAlias);
                log.debug("SQLObject: [{}]，旧的join condition：[{}]，新的join condition：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getCondition()), DruidSQLUtils.toLowerCaseSQL(condition));
                x.setCondition(condition);
            }
            clear(right);
        }
    }

    @Override
    public void endVisit(MySqlSelectQueryBlock x) {
        // 在 endVisit(SQLJoinTableSource x) 时处理了join场景，这里仅需要处理单表场景。
        SQLTableSource from = x.getFrom();
        String aTableName = getTableName(from);
        String aAlias = getAlias(from);
        if (aTableName != null) {
            if (useSubQuery) {
                SQLTableSource tableSource = newFrom(aTableName, aAlias);
                log.debug("SQLObject: [{}]，旧的tableSource：[{}]，新的tableSource：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getFrom()), DruidSQLUtils.toLowerCaseSQL(tableSource));
                x.setFrom(tableSource);
            } else {
                SQLExpr where = newWhere(x.getWhere(), aTableName, aAlias);
                log.debug("SQLObject: [{}]，旧的where：[{}]，新的where：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getWhere()), DruidSQLUtils.toLowerCaseSQL(where));
                x.setWhere(where);
            }
            clear(from);
        }
    }

    @Override
    public void endVisit(MySqlUpdateStatement x) {
        // 在 endVisit(SQLJoinTableSource x) 时处理了join场景，这里仅需要处理单表场景。
        SQLTableSource from = x.getTableSource();
        String aTableName = getTableName(from);
        String aAlias = getAlias(from);
        if (aTableName != null) {
            // update单表，只能使用where
            SQLExpr where = newWhere(x.getWhere(), aTableName, aAlias);
            log.debug("SQLObject: [{}]，旧的where：[{}]，新的where：[{}]。",
                    DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getWhere()), DruidSQLUtils.toLowerCaseSQL(where));
            x.setWhere(where);
            clear(from);
        }
    }

}
