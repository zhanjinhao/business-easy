package cn.addenda.businesseasy.jdbc.interceptor.tombstone;

import cn.addenda.businesseasy.jdbc.JdbcException;

/**
 * @author addenda
 * @since 2023/5/2 17:48
 */
public class TombstoneException extends JdbcException {

    public TombstoneException() {
    }

    public TombstoneException(String message) {
        super(message);
    }

    public TombstoneException(String message, Throwable cause) {
        super(message, cause);
    }

    public TombstoneException(Throwable cause) {
        super(cause);
    }

    public TombstoneException(String message, int errorCode) {
        super(message, errorCode);
    }

    public TombstoneException(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }

    public TombstoneException(Throwable cause, int errorCode) {
        super(cause, errorCode);
    }
}
