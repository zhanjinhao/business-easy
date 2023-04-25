package cn.addenda.businesseasy.jdbc.interceptor;

import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.proxy.jdbc.ConnectionProxy;

import java.sql.SQLException;

/**
 * @author addenda
 * @datetime 2023/4/23 19:00
 */
public interface InterceptorChain extends FilterChain {

    ConnectionProxy dataSource_connect(
            InterceptedDataSource dataSource) throws SQLException;

    ConnectionProxy dataSource_connect(
            InterceptedDataSource dataSource, String username, String password)
            throws SQLException;

}
