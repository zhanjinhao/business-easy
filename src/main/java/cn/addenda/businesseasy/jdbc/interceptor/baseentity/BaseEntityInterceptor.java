package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;
import cn.addenda.businesseasy.jdbc.interceptor.ConnectionPrepareStatementInterceptor;
import cn.addenda.businesseasy.jdbc.interceptor.InsertSelectAddItemMode;
import cn.addenda.businesseasy.jdbc.interceptor.UpdateItemMode;
import cn.addenda.businesseasy.jdbc.interceptor.tombstone.TombstoneException;
import cn.addenda.businesseasy.util.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author addenda
 * @since 2023/5/2 17:33
 */
@Slf4j
public class BaseEntityInterceptor extends ConnectionPrepareStatementInterceptor {

    private final BaseEntityRewriter baseEntityRewriter;

    private final boolean defaultReportItemNameExists;
    private final Boolean defaultDuplicateKeyUpdate;
    private final InsertSelectAddItemMode defaultInsertSelectAddItemMode;
    private final UpdateItemMode defaultUpdateItemMode;

    public BaseEntityInterceptor(BaseEntityRewriter baseEntityRewriter, InsertSelectAddItemMode insertSelectAddItemMode,
                                 boolean duplicateKeyUpdate, UpdateItemMode updateItemMode, boolean reportItemNameExists) {
        this.baseEntityRewriter = baseEntityRewriter;
        this.defaultReportItemNameExists = reportItemNameExists;
        this.defaultDuplicateKeyUpdate = duplicateKeyUpdate;
        this.defaultInsertSelectAddItemMode = insertSelectAddItemMode;
        this.defaultUpdateItemMode = updateItemMode;
    }

    @Override
    protected String process(String sql) {
        log.debug("Base Entity, before sql rewriting: [{}].", sql);
        try {
            if (JdbcSQLUtils.isSelect(sql)) {
                sql = baseEntityRewriter.rewriteSelectSql(sql, BaseEntityContext.getMasterView());
            } else if (JdbcSQLUtils.isUpdate(sql)) {
                Boolean reportItemNameExists =
                        JdbcSQLUtils.getOrDefault(BaseEntityContext.getReportItemNameExists(), defaultReportItemNameExists);
                UpdateItemMode updateItemMode =
                        JdbcSQLUtils.getOrDefault(BaseEntityContext.getUpdateItemMode(), defaultUpdateItemMode);
                sql = baseEntityRewriter.rewriteUpdateSql(sql, updateItemMode, reportItemNameExists);
            } else if (JdbcSQLUtils.isInsert(sql)) {
                Boolean reportItemNameExists =
                        JdbcSQLUtils.getOrDefault(BaseEntityContext.getReportItemNameExists(), defaultReportItemNameExists);
                UpdateItemMode updateItemMode =
                        JdbcSQLUtils.getOrDefault(BaseEntityContext.getUpdateItemMode(), defaultUpdateItemMode);
                Boolean duplicateKeyUpdate =
                        JdbcSQLUtils.getOrDefault(BaseEntityContext.getDuplicateKeyUpdate(), defaultDuplicateKeyUpdate);
                InsertSelectAddItemMode insertSelectAddItemMode =
                        JdbcSQLUtils.getOrDefault(BaseEntityContext.getInsertSelectAddItemMode(), defaultInsertSelectAddItemMode);
                sql = baseEntityRewriter.rewriteInsertSql(sql, insertSelectAddItemMode, duplicateKeyUpdate, updateItemMode, reportItemNameExists);
            } else {
                throw new JdbcException("仅支持select、update、delete、insert语句，当前SQL：" + sql + "。");
            }
        } catch (Throwable throwable) {
            throw new TombstoneException("基础字段填充时出错，SQL：" + sql + "。", ExceptionUtil.unwrapThrowable(throwable));
        }
        log.debug("Base Entity, after sql rewriting: [{}].", sql);
        return sql;
    }

}
