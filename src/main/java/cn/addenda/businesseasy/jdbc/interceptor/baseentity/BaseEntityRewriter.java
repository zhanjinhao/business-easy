package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;

/**
 * @author addenda
 * @since 2023/5/2 19:35
 */
public interface BaseEntityRewriter {

    default String rewriteSql(String sql, String masterView) {
        if (JdbcSQLUtils.isDelete(sql)) {
            return rewriteDeleteSql(sql);
        } else if (JdbcSQLUtils.isSelect(sql)) {
            return rewriteSelectSql(sql, masterView);
        } else if (JdbcSQLUtils.isUpdate(sql)) {
            return rewriteUpdateSql(sql);
        } else if (JdbcSQLUtils.isInsert(sql)) {
            return rewriteInsertSql(sql);
        } else {
            throw new JdbcException("仅支持select、update、delete、insert语句，当前SQL：" + sql + "。");
        }
    }

    String rewriteInsertSql(String sql);

    String rewriteDeleteSql(String sql);

    String rewriteSelectSql(String sql, String masterView);

    String rewriteUpdateSql(String sql);

}
