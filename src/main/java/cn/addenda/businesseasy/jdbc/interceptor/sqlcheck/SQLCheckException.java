package cn.addenda.businesseasy.jdbc.interceptor.sqlcheck;

import cn.addenda.businesseasy.jdbc.JdbcException;

/**
 * @author addenda
 * @since 2023/5/7 15:53
 */
public class SQLCheckException extends JdbcException {

    public SQLCheckException() {
    }

    public SQLCheckException(String message) {
        super(message);
    }

    public SQLCheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public SQLCheckException(Throwable cause) {
        super(cause);
    }

    public SQLCheckException(String message, int errorCode) {
        super(message, errorCode);
    }

    public SQLCheckException(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }

    public SQLCheckException(Throwable cause, int errorCode) {
        super(cause, errorCode);
    }
}
