package cn.addenda.businesseasy;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 17:28
 */
public class BusinessEasyException extends RuntimeException {

    private int errorCode = 0;

    public BusinessEasyException() {
    }

    public BusinessEasyException(String message) {
        super(message);
        errorCode = 0;
    }

    public BusinessEasyException(String message, Throwable cause) {
        super(message, cause);
        errorCode = 0;
    }

    public BusinessEasyException(Throwable cause) {
        super(cause);
        errorCode = 0;
    }

    public BusinessEasyException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessEasyException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BusinessEasyException(Throwable cause, int errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return super.toString() + " error code is " + getErrorCode() + "!";
    }
}
