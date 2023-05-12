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

    private TombstoneSqlRewriter tombstoneSqlRewriter;

    public TombstoneInterceptor(List<String> tombstoneTableNameList) {
        this.tombstoneSqlRewriter = new DruidTombstoneSqlRewriter(tombstoneTableNameList, false, true);
    }

    public TombstoneInterceptor(TombstoneSqlRewriter tombstoneSqlRewriter) {
        this.tombstoneSqlRewriter = tombstoneSqlRewriter;
    }

    @Override
    protected String process(String sql) {
        log.debug("Tombstone, before sql rewriting: [{}].", sql);
        try {
            sql = tombstoneSqlRewriter.rewriteSql(sql);
        } catch (Throwable throwable) {
            throw new TombstoneException("物理删除改写为逻辑删除时出错，SQL：" + sql + "。", ExceptionUtil.unwrapThrowable(throwable));
        }
        log.debug("Tombstone, after sql rewriting: [{}].", sql);
        return sql;
    }

}
