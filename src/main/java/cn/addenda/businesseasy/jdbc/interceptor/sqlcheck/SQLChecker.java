package cn.addenda.businesseasy.jdbc.interceptor.sqlcheck;

/**
 * @author addenda
 * @since 2023/5/7 19:56
 */
public interface SQLChecker {

    boolean exactIdentifier(String sql);

    boolean allColumnExists(String sql);

}
