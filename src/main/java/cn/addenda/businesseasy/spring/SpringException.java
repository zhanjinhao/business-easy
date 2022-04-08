package cn.addenda.businesseasy.spring;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @author ISJINHAO
 * @date 2022/4/8
 */
public class SpringException extends BusinessEasyException {

    public SpringException(String message) {
        super(message, 1007);
    }

    public SpringException(String message, Throwable e) {
        super(message, e, 1007);
    }

}
