package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.proxy.jdbc.ConnectionProxy;

import java.sql.SQLException;

/**
 * @author addenda
 * @datetime 2023/4/23 18:57
 */
public interface Interceptor extends Filter {

    ConnectionProxy dataSource_getConnection(
            InterceptorChain chain, InterceptedDataSource dataSource)
            throws SQLException;

    ConnectionProxy dataSource_getConnection(
            InterceptorChain chain, InterceptedDataSource dataSource, String username, String password)
            throws SQLException;

}
