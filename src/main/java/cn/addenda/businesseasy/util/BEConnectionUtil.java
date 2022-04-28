package cn.addenda.businesseasy.util;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 01395265
 * @date 2022/4/28
 */
@Slf4j
public class BEConnectionUtil {

    private BEConnectionUtil() {

    }

    public static boolean close(Connection connection) {
        try {
            connection.close();
            return true;
        } catch (SQLException e) {
            log.error("关闭 Connection 失败，connection: {}. ", connection, e);
        }
        return false;
    }

    public static Connection openConnection(DataSource dataSource) {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.error("无法从dataSource中获取connection，dataSource: {}", dataSource, e);
        }
        return null;
    }

    public static boolean rollback(Connection connection) {
        try {
            connection.rollback();
            return true;
        } catch (SQLException e) {
            log.error("rollback 失败，connection: {}", connection, e);
        }
        return false;
    }

    public static boolean commit(Connection connection) {
        try {
            connection.commit();
            return true;
        } catch (SQLException e) {
            log.error("commit 失败，connection: {}", connection, e);
        }
        return false;
    }

    public static int setAutoCommit(Connection connection, boolean expect) {
        try {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(expect);
            return autoCommit ? 1 : 0;
        } catch (SQLException e) {
            log.error("设置 connection {} 的 autoCommit 为 {} 失败！", connection, expect, e);
        }
        return -1;
    }

    public static int setTransactionIsolation(Connection connection, int expect) {
        try {
            int transactionIsolation = connection.getTransactionIsolation();
            connection.setTransactionIsolation(expect);
            return transactionIsolation;
        } catch (SQLException e) {
            log.error("设置 connection {} 的 事务隔离级别为 {} 失败！", connection, expect, e);
        }
        return -1;
    }

}
