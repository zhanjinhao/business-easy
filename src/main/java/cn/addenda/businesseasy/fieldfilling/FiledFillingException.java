package cn.addenda.businesseasy.fieldfilling;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @Author ISJINHAO
 * @Date 2022/2/1 17:33
 */
public class FiledFillingException extends BusinessEasyException {

    public FiledFillingException(String message) {
        super(message, 1001);
    }

    public FiledFillingException(String message, Throwable e) {
        super(message, e, 1001);
    }

}
