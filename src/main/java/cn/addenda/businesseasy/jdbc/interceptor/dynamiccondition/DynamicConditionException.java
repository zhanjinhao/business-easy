package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

import cn.addenda.businesseasy.jdbc.JdbcException;

/**
 * @author addenda
 * @since 2022/11/26 22:36
 */
public class DynamicConditionException extends JdbcException {

    public DynamicConditionException() {
    }

    public DynamicConditionException(String message) {
        super(message);
    }

    public DynamicConditionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DynamicConditionException(Throwable cause) {
        super(cause);
    }

    public DynamicConditionException(String message, int errorCode) {
        super(message, errorCode);
    }

    public DynamicConditionException(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }

    public DynamicConditionException(Throwable cause, int errorCode) {
        super(cause, errorCode);
    }
}
