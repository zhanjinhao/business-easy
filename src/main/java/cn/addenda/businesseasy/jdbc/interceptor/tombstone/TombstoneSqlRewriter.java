package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

/**
 * @author addenda
 * @since 2023/4/30 19:38
 */
public interface TombstoneSqlRewriter {

    String rewriteInsertSql(String sql, boolean useSubQuery);

    String rewriteDeleteSql(String sql);

    String rewriteSelectSql(String sql, boolean useSubQuery);

    String rewriteUpdateSql(String sql);

}
