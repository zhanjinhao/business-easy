package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @Author ISJINHAO
 * @Date 2022/4/9 15:46
 */
public class CdcException extends BusinessEasyException {

    public CdcException(String message) {
        super(message, 1010);
    }

    public CdcException(String message, Throwable cause) {
        super(message, cause, 1010);
    }

}
