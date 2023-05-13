package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

import cn.addenda.businesseasy.jdbc.interceptor.ConnectionPrepareStatementInterceptor;
import cn.addenda.businesseasy.util.ExceptionUtil;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author addenda
 * @since 2023/5/2 17:33
 */
@Slf4j
public class TombstoneInterceptor extends ConnectionPrepareStatementInterceptor {

    private final TombstoneSqlRewriter tombstoneSqlRewriter;

    public TombstoneInterceptor(List<String> tombstoneTableNameList) {
        this.tombstoneSqlRewriter = new DruidTombstoneSqlRewriter();
    }

    public TombstoneInterceptor(TombstoneSqlRewriter tombstoneSqlRewriter, boolean removeEnter) {
        super(removeEnter);
        this.tombstoneSqlRewriter = tombstoneSqlRewriter;
    }

    @Override
    protected String process(String sql) {
        log.debug("Tombstone, before sql rewriting: [{}].", removeEnter(sql));
        try {
            sql = tombstoneSqlRewriter.rewriteSql(sql);
        } catch (Throwable throwable) {
            String msg = String.format("物理删除改写为逻辑删除时出错，SQL：[%s]。", removeEnter(sql));
            throw new TombstoneException(msg, ExceptionUtil.unwrapThrowable(throwable));
        }
        log.debug("Tombstone, after sql rewriting: [{}].", removeEnter(sql));
        return sql;
    }

}
