package cn.addenda.businesseasy.jdbc.visitor.condition;

import cn.addenda.businesseasy.jdbc.DruidSQLUtils;
import cn.addenda.businesseasy.util.BEArrayUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
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

    private final boolean joinUseSubQuery;

    private final boolean rewriteCommaToJoin;

    public TableAddJoinConditionVisitor(String condition) {
        super(null, null, condition);
        this.joinUseSubQuery = false;
        this.rewriteCommaToJoin = true;
    }

    public TableAddJoinConditionVisitor(String tableName, String condition) {
        super(tableName == null ? null : BEArrayUtils.asArrayList(tableName), null, condition);
        this.joinUseSubQuery = false;
        this.rewriteCommaToJoin = true;
    }

    public TableAddJoinConditionVisitor(String tableName, String condition, boolean joinUseSubQuery) {
        super(tableName == null ? null : BEArrayUtils.asArrayList(tableName), null, condition);
        this.joinUseSubQuery = joinUseSubQuery;
        this.rewriteCommaToJoin = true;
    }

    public TableAddJoinConditionVisitor(
            List<String> included, List<String> notIncluded, String condition, boolean joinUseSubQuery, boolean rewriteCommaToJoin) {
        super(included, notIncluded, condition);
        this.joinUseSubQuery = joinUseSubQuery;
        this.rewriteCommaToJoin = rewriteCommaToJoin;
    }

    @Override
    public void endVisit(SQLJoinTableSource x) {
        JoinType joinType = x.getJoinType();
        if (joinType == JoinType.COMMA && rewriteCommaToJoin) {
            // A,B -> 改写为 A JOIN B
            x.setJoinType(JoinType.JOIN);
            joinType = JoinType.JOIN;
        }

        SQLTableSource left = x.getLeft();
        inherit(x, left);

        String leftTableName = getTableName(left);
        String leftAlias = getAlias(left);
        if (leftTableName != null) {
            if (joinUseSubQuery) {
                SQLTableSource tableSource = newFrom(leftTableName, leftAlias);
                log.debug("SQLObject: [{}]，旧的tableSource：[{}]，新的tableSource：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getLeft()), DruidSQLUtils.toLowerCaseSQL(tableSource));
                x.setLeft(tableSource);
            } else {
                if (JoinType.COMMA == joinType || JoinType.LEFT_OUTER_JOIN == joinType) {
                    // 上升到where
                    addWhereTableName(x, leftTableName);
                    addWhereAlias(x, leftAlias);
                } else if (JoinType.RIGHT_OUTER_JOIN == joinType || JoinType.JOIN == joinType || JoinType.INNER_JOIN == joinType) {
                    // 条件加在on上
                    SQLExpr condition = newWhere(x.getCondition(), leftTableName, leftAlias);
                    log.debug("SQLObject: [{}]，旧的join condition：[{}]，新的join condition：[{}]。",
                            DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getCondition()), DruidSQLUtils.toLowerCaseSQL(condition));
                    x.setCondition(condition);
                } else {
                    // cross join 、nature join 等其他场景，使用子查询
                    SQLTableSource tableSource = newFrom(leftTableName, leftAlias);
                    log.debug("SQLObject: [{}]，旧的tableSource：[{}]，新的tableSource：[{}]。",
                            DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getLeft()), DruidSQLUtils.toLowerCaseSQL(tableSource));
                    x.setLeft(tableSource);
                }
            }
            clear(left);
        }

        SQLTableSource right = x.getRight();
        inherit(x, right);

        String rightTableName = getTableName(right);
        String rightAlias = getAlias(right);
        if (rightTableName != null) {
            if (joinUseSubQuery) {
                SQLTableSource tableSource = newFrom(rightTableName, rightAlias);
                log.debug("SQLObject: [{}]，旧的tableSource：[{}]，新的tableSource：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getRight()), DruidSQLUtils.toLowerCaseSQL(tableSource));
                x.setRight(tableSource);
            } else {
                if (JoinType.COMMA == joinType || JoinType.RIGHT_OUTER_JOIN == joinType) {
                    // 上升到where
                    addWhereTableName(x, rightTableName);
                    addWhereAlias(x, rightAlias);
                } else if (JoinType.LEFT_OUTER_JOIN == joinType || JoinType.JOIN == joinType || JoinType.INNER_JOIN == joinType) {
                    // 条件加在on上
                    SQLExpr condition = newWhere(x.getCondition(), rightTableName, rightAlias);
                    log.debug("SQLObject: [{}]，旧的join condition：[{}]，新的join condition：[{}]。",
                            DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getCondition()), DruidSQLUtils.toLowerCaseSQL(condition));
                    x.setCondition(condition);
                } else {
                    // cross join 、nature join 等其他场景，使用子查询
                    SQLTableSource tableSource = newFrom(rightTableName, rightAlias);
                    log.debug("SQLObject: [{}]，旧的tableSource：[{}]，新的tableSource：[{}]。",
                            DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getRight()), DruidSQLUtils.toLowerCaseSQL(tableSource));
                    x.setRight(tableSource);
                }
            }
            clear(right);
        }
    }

    private void inherit(SQLObject x, SQLTableSource tableSource) {
        String whereRightTableName = getWhereTableName(tableSource);
        String whereRightAlias = getWhereAlias(tableSource);
        if (whereRightTableName != null) {
            addWhereTableName(x, whereRightTableName);
            addWhereAlias(x, whereRightAlias);
        }
    }

    @Override
    public void endVisit(MySqlSelectQueryBlock x) {
        SQLTableSource from = x.getFrom();

        if (from instanceof SQLJoinTableSource) {
            String whereTableName = getWhereTableName(from);
            String whereAlias = getWhereAlias(from);
            if (whereTableName != null) {
                SQLExpr where = newWhere(x.getWhere(), whereTableName, whereAlias);
                log.debug("SQLObject: [{}]，旧的where：[{}]，新的where：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getWhere()), DruidSQLUtils.toLowerCaseSQL(where));
                x.setWhere(where);
            }
        }
        // SQLExprTableSource 处理单表场景
        else if (from instanceof SQLExprTableSource) {
            String aTableName = getTableName(from);
            String aAlias = getAlias(from);
            if (aTableName != null) {
                SQLExpr where = newWhere(x.getWhere(), aTableName, aAlias);
                log.debug("SQLObject: [{}]，旧的where：[{}]，新的where：[{}]。",
                        DruidSQLUtils.toLowerCaseSQL(x), DruidSQLUtils.toLowerCaseSQL(x.getWhere()), DruidSQLUtils.toLowerCaseSQL(where));
                x.setWhere(where);
            }
        }
        clear(from);
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

    private static final String WHERE_TABLE_NAME_KEY = "WHERE_TABLE_NAME_KEY";
    private static final String WHERE_ALIAS_KEY = "WHERE_ALIAS_KEY";

    protected String getWhereTableName(SQLObject sqlObject) {
        return (String) sqlObject.getAttribute(WHERE_TABLE_NAME_KEY);
    }

    protected String getWhereAlias(SQLObject sqlObject) {
        return (String) sqlObject.getAttribute(WHERE_ALIAS_KEY);
    }

    protected void addWhereTableName(SQLObject sqlObject, String tableName) {
        sqlObject.putAttribute(WHERE_TABLE_NAME_KEY, tableName);
    }

    protected void addWhereAlias(SQLObject sqlObject, String alias) {
        sqlObject.putAttribute(WHERE_ALIAS_KEY, alias);
    }

    @Override
    public String toString() {
        return "TableAddJoinConditionVisitor{" +
                "useSubQuery=" + joinUseSubQuery +
                ", rewriteCommaToJoin=" + rewriteCommaToJoin +
                "} " + super.toString();
    }

}
