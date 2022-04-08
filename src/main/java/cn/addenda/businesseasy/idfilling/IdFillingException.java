package cn.addenda.businesseasy.idfilling;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 22:45
 */
public class IdFillingException extends BusinessEasyException {

    public IdFillingException(String message) {
        super(message, 1002);
    }

    public IdFillingException(String message, Throwable e) {
        super(message, e, 1002);
    }

}
