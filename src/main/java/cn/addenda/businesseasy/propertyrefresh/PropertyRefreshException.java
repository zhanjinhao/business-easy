package cn.addenda.businesseasy.propertyrefresh;

/**
 * @Author ISJINHAO
 * @Date 2022/4/4 11:42
 */
public class PropertyRefreshException extends RuntimeException {

    public PropertyRefreshException() {
    }

    public PropertyRefreshException(String message) {
        super(message);
    }

    public PropertyRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}
