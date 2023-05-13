package cn.addenda.businesseasy.jdbc.interceptor.dynamicsql;

import cn.addenda.businesseasy.jdbc.JdbcException;

/**
 * @author addenda
 * @since 2022/11/26 22:36
 */
public class DynamicSQLException extends JdbcException {

    public DynamicSQLException() {
    }

    public DynamicSQLException(String message) {
        super(message);
    }

    public DynamicSQLException(String message, Throwable cause) {
        super(message, cause);
    }

    public DynamicSQLException(Throwable cause) {
        super(cause);
    }

    public DynamicSQLException(String message, int errorCode) {
        super(message, errorCode);
    }

    public DynamicSQLException(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }

    public DynamicSQLException(Throwable cause, int errorCode) {
        super(cause, errorCode);
    }
}
