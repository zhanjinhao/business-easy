package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

import cn.addenda.businesseasy.jdbc.interceptor.ConnectionPrepareStatementInterceptor;
import cn.addenda.businesseasy.util.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

/**
 * @author addenda
 * @datetime 2023/4/30 16:30
 */
@Slf4j
public class DynamicConditionInterceptor extends ConnectionPrepareStatementInterceptor {

    private DynamicConditionAssembler dynamicConditionAssembler;

    public DynamicConditionInterceptor(DynamicConditionAssembler dynamicConditionAssembler) {
        this.dynamicConditionAssembler = dynamicConditionAssembler;
    }

    protected String process(String sql) {
        Map<String, String> viewConstraints = DynamicConditionContext.getViewConditions();
        Map<String, String> tableConstraints = DynamicConditionContext.getTableConditions();

        if (viewConstraints == null && tableConstraints == null) {
            return sql;
        }
        log.debug("Dynamic Condition, before sql rewriting: [{}].", sql);
        String newSql;
        try {
            newSql = doProcess(sql, viewConstraints, tableConstraints);
        } catch (Throwable throwable) {
            throw new DynamicConditionException(
                    "拼装动态条件时出错，SQL：" + sql
                            + "，viewConstraints：" + viewConstraints
                            + "，tableConstraints" + tableConstraints + "。",
                    ExceptionUtil.unwrapThrowable(throwable));
        }

        log.debug("Dynamic Condition, after sql rewriting: [{}].", newSql);
        return newSql;
    }

    private String doProcess(String sql, Map<String, String> viewConstraints, Map<String, String> tableConstraints) {
        String newSql = sql;

        // view 过滤条件
        if (viewConstraints != null && !viewConstraints.isEmpty()) {
            newSql = apply(sql, viewConstraints,
                    (t1, t2, t3) -> dynamicConditionAssembler.viewAddCondition(t1, t2, t3));
        }

        // table 过滤条件
        if (tableConstraints != null && !tableConstraints.isEmpty()) {
            newSql = apply(sql, tableConstraints,
                    (t1, t2, t3) -> dynamicConditionAssembler.tableAddCondition(t1, t2, t3));
        }

        return newSql;
    }

    private String apply(String sql, Map<String, String> constraints, TernaryOperator<String> operator) {
        String newSql = sql;
        Set<Map.Entry<String, String>> entries = constraints.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            newSql = operator.apply(newSql, entry.getKey(), entry.getValue());
        }
        return newSql;
    }

    private interface TernaryOperator<T> {
        T apply(T t1, T t2, T t3);
    }

}
