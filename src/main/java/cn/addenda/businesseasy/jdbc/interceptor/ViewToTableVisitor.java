package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 最终的结果有三种场景：<br/>
 * <pre>
 * - tableName -> tableName：select n from a
 * - alias -> tableName：select n from a t
 * - alias -> null： select n from (select n from a) t; select n from (select n from a union select n from b) t
 * </pre>
 *
 * @author addenda
 * @datetime 2023/5/03 20:09
 */
@Slf4j
public class ViewToTableVisitor extends SQLASTVisitorAdapter {

    public static final String VIEW_TO_TABLE_KEY = "view_to_table_key";

    /**
     * <pre>{@code
     *
     *             with tmp1 as
     *                     (select e1.deptno, round(avg(ifnull(e1.sal, 0)), 2) avg_sal
     *                             from emp e1
     *                             group by e1.deptno),
     *                     tmp2 as
     *                     (select e1.deptno, round(avg(ifnull(e1.sal, 0)), 2) avg_sal
     *                             from emp e1
     *                             where e1.sal > 1000
     *                             group by e1.deptno)
     *             select d.deptno, tmp1.avg_sal avg_sal1, tmp2.avg_sal avg_sal2
     *             from dept d
     *             left join tmp1
     *             on d.deptno = tmp1.deptno
     *             left join tmp2
     *             on d.deptno = tmp2.deptno
     *     }
     * </pre>
     */
    @Override
    public void endVisit(SQLWithSubqueryClause x) {
        // 不支持
    }

    /**
     * <pre>{@code
     *     select a from  log, unnest(cast(json_parse(number) as array(bigint))) as t(a)
     * }
     * </pre>
     */
    @Override
    public void endVisit(SQLUnnestTableSource x) {
        // 不支持
    }

    /**
     * <pre>{@code
     *     select c from (values (1), (2), (3)) as t(c)
     * }
     * </pre>
     */
    @Override
    public void endVisit(SQLValuesTableSource x) {
        // 不支持
    }

    @Override
    public void endVisit(SQLExprTableSource x) {
        String alias = x.getAlias();
        String tableName = x.getTableName();
        String view = alias == null ? tableName : alias;

        // 设置view_to_table
        Map<String, String> map = new HashMap<>();
        map.put(view, tableName);
        x.putAttribute(VIEW_TO_TABLE_KEY, map);
        log.debug("SQLObject: [{}], viewToTableMap: [{}].", DruidSQLUtils.toLowerCaseSQL(x), map);
    }

    @Override
    public void endVisit(SQLUnionQueryTableSource x) {
        String alias = x.getAlias();

        // 设置view_to_table
        Map<String, String> map = new HashMap<>();
        map.put(alias, null);
        x.putAttribute(VIEW_TO_TABLE_KEY, map);
        log.debug("SQLObject: [{}], viewToTableMap: [{}].", DruidSQLUtils.toLowerCaseSQL(x), map);
    }

    @Override
    public void endVisit(SQLSubqueryTableSource x) {
        String alias = x.getAlias();

        // 设置view_to_table
        Map<String, String> map = new HashMap<>();
        map.put(alias, null);
        x.putAttribute(VIEW_TO_TABLE_KEY, map);
        log.debug("SQLObject: [{}], viewToTableMap: [{}].", DruidSQLUtils.toLowerCaseSQL(x), map);
    }

    @Override
    public void endVisit(SQLJoinTableSource x) {
        SQLTableSource left = x.getLeft();
        SQLTableSource right = x.getRight();

        // 设置view_to_table
        Map<String, String> leftMap = getViewToTableMap(left);
        Map<String, String> rightMap = getViewToTableMap(right);
        Map<String, String> map = new HashMap<>();
        if (leftMap != null) {
            map.putAll(leftMap);
        }
        if (rightMap != null) {
            map.putAll(rightMap);
        }
        if (!map.isEmpty()) {
            x.putAttribute(VIEW_TO_TABLE_KEY, map);
            log.debug("SQLObject: [{}], viewToTableMap: [{}].", DruidSQLUtils.toLowerCaseSQL(x), map);
        }
    }

    @Override
    public void endVisit(SQLSelectQueryBlock x) {
        Map<String, String> viewToTableMap = getViewToTableMap(x.getFrom());
        if (viewToTableMap != null) {
            Map<String, String> map = new HashMap<>(viewToTableMap);
            x.putAttribute(VIEW_TO_TABLE_KEY, map);
            log.debug("SQLObject: [{}], viewToTableMap: [{}].", DruidSQLUtils.toLowerCaseSQL(x), map);
        }
    }

    @Override
    public void endVisit(SQLUnionQuery x) {
        List<SQLSelectQuery> relations = x.getRelations();
        Map<String, String> map = new HashMap<>();
        for (SQLSelectQuery sqlSelectQuery : relations) {
            map.putAll(getViewToTableMap(sqlSelectQuery));
        }
        x.putAttribute(VIEW_TO_TABLE_KEY, map);
        log.debug("SQLObject: [{}], viewToTableMap: [{}].", DruidSQLUtils.toLowerCaseSQL(x), map);
    }

    public static Map<String, String> getViewToTableMap(SQLObject sqlObject) {
        return (Map<String, String>) sqlObject.getAttribute(VIEW_TO_TABLE_KEY);
    }

}
