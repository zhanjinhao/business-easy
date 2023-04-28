package cn.addenda.businesseasy.jdbc.interceptor.lockingreads;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @author addenda
 * @datetime 2022/10/11 19:19
 */
public class LockingReadsException extends BusinessEasyException {

    public LockingReadsException(String message) {
        super(message);
    }

    public LockingReadsException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockingReadsException(Throwable cause) {
        super(cause);
    }

}
