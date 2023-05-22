package cn.addenda.businesseasy.jdbc.interceptor.sqlcheck;

/**
 * todo 支持 where 条件检测
 *
 * @author addenda
 * @since 2023/5/7 19:56
 */
public interface SQLChecker {

    boolean exactIdentifier(String sql);

    boolean allColumnExists(String sql);

}
