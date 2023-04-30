package cn.addenda.businesseasy.jdbc.interceptor.lockingreads;

import cn.addenda.businesseasy.jdbc.interceptor.ConnectionPrepareStatementInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author addenda
 * @since 2023/4/27 20:19
 */
@Slf4j
public class LockingReadsInterceptor extends ConnectionPrepareStatementInterceptor {

    protected String process(String sql) {
        log.debug("Locking Reads, before sql rewriting: [{}].", sql);
        String lock = LockingReadsContext.getLock();
        if (lock == null) {
            // no-op
        } else if (LockingReadsContext.R_LOCK.equals(lock)) {
            sql = sql + " lock in share mode";
        } else if (LockingReadsContext.W_LOCK.equals(lock)) {
            sql = sql + " for update";
        } else {
            throw new LockingReadsException("不支持的LOCK类型，当前LOCK类型：" + lock + "。");
        }

        log.debug("Locking Reads, after sql rewriting: [{}].", sql);
        return sql;
    }

}
