package cn.addenda.businesseasy.propertyrefresh;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @Author ISJINHAO
 * @Date 2022/4/4 11:42
 */
public class PropertyRefreshException extends BusinessEasyException {

    public PropertyRefreshException() {
    }

    public PropertyRefreshException(String message) {
        super(message, 1004);
    }

    public PropertyRefreshException(String message, Throwable cause) {
        super(message, cause, 1004);
    }
}
