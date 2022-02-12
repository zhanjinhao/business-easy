package cn.addenda.businesseasy.exception;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 17:28
 */
public class BusinessException extends RuntimeException {

    private int errorCode = 0;

    public BusinessException(String message) {
        super(message);
        errorCode = 0;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        errorCode = 0;
    }

    public BusinessException(Throwable cause) {
        super(cause);
        errorCode = 0;
    }

    public BusinessException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BusinessException(Throwable cause, int errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
