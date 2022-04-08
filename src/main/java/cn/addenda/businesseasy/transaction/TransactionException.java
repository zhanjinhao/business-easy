package cn.addenda.businesseasy.transaction;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @author ISJINHAO
 * @date 2022/4/8
 */
public class TransactionException extends BusinessEasyException {

    public TransactionException(String message) {
        super(message, 1008);
    }

    public TransactionException(String message, Throwable e) {
        super(message, e, 1008);
    }

}
