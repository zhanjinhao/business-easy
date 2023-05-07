package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import cn.addenda.businesseasy.jdbc.JdbcException;

/**
 * @author addenda
 * @since 2023/5/2 19:37
 */
public class BaseEntityException extends JdbcException {
    public BaseEntityException() {
    }

    public BaseEntityException(String message) {
        super(message);
    }

    public BaseEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseEntityException(Throwable cause) {
        super(cause);
    }

    public BaseEntityException(String message, int errorCode) {
        super(message, errorCode);
    }

    public BaseEntityException(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }

    public BaseEntityException(Throwable cause, int errorCode) {
        super(cause, errorCode);
    }
}
