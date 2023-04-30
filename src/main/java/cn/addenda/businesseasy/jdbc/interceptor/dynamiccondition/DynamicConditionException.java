package cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @author addenda
 * @since 2022/11/26 22:36
 */
public class DynamicConditionException extends BusinessEasyException {

    public DynamicConditionException(String message) {
        super(message);
    }

    public DynamicConditionException(String message, Throwable cause) {
        super(message, cause);
    }
}
