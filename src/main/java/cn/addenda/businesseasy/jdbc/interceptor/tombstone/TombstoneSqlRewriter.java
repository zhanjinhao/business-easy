package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

import cn.addenda.businesseasy.jdbc.JdbcException;
import cn.addenda.businesseasy.jdbc.JdbcSQLUtils;

/**
 * @author addenda
 * @since 2023/4/30 19:38
 */
public interface TombstoneSqlRewriter {

    default String rewriteSql(String sql) {
        if (JdbcSQLUtils.isDelete(sql)) {
            return rewriteDeleteSql(sql);
        } else if (JdbcSQLUtils.isSelect(sql)) {
            return rewriteSelectSql(sql);
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

    String rewriteSelectSql(String sql);

    String rewriteUpdateSql(String sql);

}
