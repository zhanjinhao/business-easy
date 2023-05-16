package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.interceptor.ConnectionPrepareStatementInterceptor;
import cn.addenda.businesseasy.jdbc.interceptor.InsertOrUpdateAddItemVisitor;
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

    private final BaseEntityRewriter baseEntityRewriter;

    private final boolean hideBaseEntity;

    private final BaseEntitySource baseEntitySource;

    public BaseEntityInterceptor(
            BaseEntityRewriter baseEntityRewriter, BaseEntitySource baseEntitySource, boolean hideBaseEntity) {
        this.baseEntityRewriter = baseEntityRewriter;
        this.baseEntitySource = baseEntitySource;
        this.hideBaseEntity = hideBaseEntity;
    }

    public BaseEntityInterceptor(
            List<String> included, List<String> notInclude, BaseEntitySource baseEntitySource,
            boolean hideBaseEntity, InsertOrUpdateAddItemVisitor.AddItemMode addItemMode) {
        this.baseEntityRewriter = new DruidBaseEntityRewriter(included, notInclude, baseEntitySource, hideBaseEntity, addItemMode);
        this.baseEntitySource = baseEntitySource;
        this.hideBaseEntity = hideBaseEntity;
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
