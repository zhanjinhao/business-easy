package cn.addenda.businesseasy.result;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @author ISJINHAO
 * @date 2022/4/8
 */
public class ResultException extends BusinessEasyException {

    public ResultException(String message) {
        super(message, 1005);
    }

    public ResultException(String message, Throwable e) {
        super(message, e, 1005);
    }

}
