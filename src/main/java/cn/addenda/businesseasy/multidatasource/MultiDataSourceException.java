package cn.addenda.businesseasy.multidatasource;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @author ISJINHAO
 * @date 2022/4/8
 */
public class MultiDataSourceException extends BusinessEasyException {

    public MultiDataSourceException(String message) {
        super(message, 1003);
    }

    public MultiDataSourceException(String message, Throwable e) {
        super(message, e, 1003);
    }

}
