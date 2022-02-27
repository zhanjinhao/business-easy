package cn.addenda.businesseasy.util;

/**
 * 这个异常正常情况下是不应该出现的，因为工具类的实现定义了确定的输入输出。
 * 在业务系统，由于依赖用户的输入，确定的输入往往很难达到。
 *
 * @author 01395265
 * @date 2022/2/14
 */
public class BEUtilException extends RuntimeException {

    private int errorCode = 0;

    public BEUtilException(String message) {
        super(message);
        errorCode = 0;
    }

    public BEUtilException(String message, Throwable cause) {
        super(message, cause);
        errorCode = 0;
    }

    public BEUtilException(Throwable cause) {
        super(cause);
        errorCode = 0;
    }

    public BEUtilException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BEUtilException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BEUtilException(Throwable cause, int errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
