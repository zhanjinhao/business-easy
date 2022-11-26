package cn.addenda.businesseasy.cache;

/**
 * @author addenda
 * @datetime 2022/11/24 23:11
 */
public class CacheException extends RuntimeException {

    public static final int BUSY = 1;
    public static final int ERROR = 2;

    private final int code;

    public CacheException(String message, int code) {
        super(message);
        this.code = code;
    }

    public CacheException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
