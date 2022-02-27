package cn.addenda.businesseasy.fieldfilling;

/**
 * @Author ISJINHAO
 * @Date 2022/2/1 17:33
 */
public class FiledFillingException extends RuntimeException {

    public FiledFillingException(String message) {
        super(message);
    }

    public FiledFillingException(String message, Throwable e) {
        super(message, e);
    }

}
