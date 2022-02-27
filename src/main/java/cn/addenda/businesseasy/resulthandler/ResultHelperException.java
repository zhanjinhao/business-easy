package cn.addenda.businesseasy.resulthandler;

/**
 * @Author ISJINHAO
 * @Date 2022/2/1 17:33
 */
public class ResultHelperException extends RuntimeException {

    public ResultHelperException(String message) {
        super(message);
    }

    public ResultHelperException(String message, Throwable e) {
        super(message, e);
    }

}
