package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.ConnectionPrepareStatementInterceptor;
import cn.addenda.businesseasy.util.ExceptionUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author addenda
 * @since 2023/5/2 17:33
 */
@Slf4j
public class TombstoneInterceptor extends ConnectionPrepareStatementInterceptor {

    private final boolean defaultJoinUseSubQuery;
    private final TombstoneSqlRewriter tombstoneSqlRewriter;

    public TombstoneInterceptor(TombstoneSqlRewriter tombstoneSqlRewriter, boolean removeEnter, boolean useSubQuery) {
        super(removeEnter);
        this.tombstoneSqlRewriter = tombstoneSqlRewriter;
        this.defaultJoinUseSubQuery = useSubQuery;
    }

    @Override
    protected String process(String sql) {
        log.debug("Tombstone, before sql rewriting: [{}].", removeEnter(sql));
        try {
            if (JdbcSQLUtils.isDelete(sql)) {
                sql = tombstoneSqlRewriter.rewriteDeleteSql(sql);
            } else if (JdbcSQLUtils.isSelect(sql)) {
                Boolean useSubQuery =
                        JdbcSQLUtils.getOrDefault(TombstoneContext.getJoinUseSubQuery(), defaultJoinUseSubQuery);
                sql = tombstoneSqlRewriter.rewriteSelectSql(sql, useSubQuery);
            } else if (JdbcSQLUtils.isUpdate(sql)) {
                sql = tombstoneSqlRewriter.rewriteUpdateSql(sql);
            } else if (JdbcSQLUtils.isInsert(sql)) {
                Boolean useSubQuery =
                        JdbcSQLUtils.getOrDefault(TombstoneContext.getJoinUseSubQuery(), defaultJoinUseSubQuery);
                sql = tombstoneSqlRewriter.rewriteInsertSql(sql, useSubQuery);
            } else {
                throw new JdbcException("仅支持select、update、delete、insert语句，当前SQL：" + sql + "。");
            }
        } catch (Throwable throwable) {
            String msg = String.format("物理删除改写为逻辑删除时出错，SQL：[%s]。", removeEnter(sql));
            throw new TombstoneException(msg, ExceptionUtil.unwrapThrowable(throwable));
        }
        log.debug("Tombstone, after sql rewriting: [{}].", removeEnter(sql));
        return sql;
    }

}
