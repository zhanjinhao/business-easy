package cn.addenda.businesseasy.cdc;

import java.sql.SQLException;

/**
 * @author addenda
 * @datetime 2022/8/27 17:01
 */
public interface StatementFunction<R> {

    R invoke() throws SQLException;

}
