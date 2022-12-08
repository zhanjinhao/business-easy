package cn.addenda.businesseasy.lock;

/**
 * @author addenda
 * @datetime 2022/12/8 23:21
 */
public class LockException extends RuntimeException {

    public LockException() {
    }

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }
}
