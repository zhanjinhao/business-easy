package cn.addenda.businesseasy.asynctask;

/**
 * @author 01395265
 * @date 2022/5/24
 */
public class AsyncTaskException extends RuntimeException {

    public AsyncTaskException(Throwable cause) {
        super(cause);
    }

    public AsyncTaskException(String message) {
        super(message);
    }

}
