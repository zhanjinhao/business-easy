package cn.addenda.businesseasy.util;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * 这个异常正常情况下是不应该出现的，因为工具类的实现定义了确定的输入输出。
 * 在业务系统，由于依赖用户的输入，确定的输入往往很难达到。
 *
 * @author ISJINHAO
 * @date 2022/2/14
 */
public class BEUtilsException extends BusinessEasyException {

    public BEUtilsException(String message) {
        super(message, 1009);
    }

    public BEUtilsException(String message, Throwable cause) {
        super(message, cause, 1009);
    }

}
