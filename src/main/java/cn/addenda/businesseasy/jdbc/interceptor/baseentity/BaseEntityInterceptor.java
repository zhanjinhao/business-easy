package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.interceptor.ConnectionPrepareStatementInterceptor;
import cn.addenda.businesseasy.jdbc.interceptor.tombstone.TombstoneException;
import cn.addenda.businesseasy.util.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author addenda
 * @since 2023/5/2 17:33
 */
@Slf4j
public class BaseEntityInterceptor extends ConnectionPrepareStatementInterceptor {

    private BaseEntityRewriter baseEntityRewriter;

    public BaseEntityInterceptor(List<String> baseEntityTableNameList, BaseEntitySource baseEntitySource) {
        this.baseEntityRewriter = new DruidBaseEntityRewriter(baseEntityTableNameList, baseEntitySource);
    }

    public BaseEntityInterceptor(BaseEntityRewriter baseEntityRewriter) {
        this.baseEntityRewriter = baseEntityRewriter;
    }

    @Override
    protected String process(String sql) {
        log.debug("Base Entity, before sql rewriting: [{}].", sql);
        try {
            sql = baseEntityRewriter.rewriteSql(sql, BaseEntityContext.getMasterView());
        } catch (Throwable throwable) {
            throw new TombstoneException("基础字段填充时出错，SQL：" + sql + "。", ExceptionUtil.unwrapThrowable(throwable));
        }
        log.debug("Base Entity, after sql rewriting: [{}].", sql);
        return sql;
    }

}
