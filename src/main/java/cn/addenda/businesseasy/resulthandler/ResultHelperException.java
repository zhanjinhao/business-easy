package cn.addenda.businesseasy.resulthandler;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @Author ISJINHAO
 * @Date 2022/2/1 17:33
 */
public class ResultHelperException extends BusinessEasyException {

    public ResultHelperException(String message) {
        super(message, 1006);
    }

    public ResultHelperException(String message, Throwable e) {
        super(message, e, 1006);
    }

}
