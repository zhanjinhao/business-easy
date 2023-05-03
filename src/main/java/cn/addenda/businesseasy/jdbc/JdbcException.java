package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @author addenda
 * @since 2023/5/2 19:43
 */
public class JdbcException extends BusinessEasyException {

    public JdbcException() {
    }

    public JdbcException(String message) {
        super(message);
    }

    public JdbcException(String message, Throwable cause) {
        super(message, cause);
    }

    public JdbcException(Throwable cause) {
        super(cause);
    }

    public JdbcException(String message, int errorCode) {
        super(message, errorCode);
    }

    public JdbcException(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }

    public JdbcException(Throwable cause, int errorCode) {
        super(cause, errorCode);
    }
}
