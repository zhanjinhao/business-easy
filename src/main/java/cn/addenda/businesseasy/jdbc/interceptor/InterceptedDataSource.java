package cn.addenda.businesseasy.jdbc.interceptor;

import cn.addenda.businesseasy.jdbc.WrapperAdapter;
import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.proxy.jdbc.*;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author addenda
 * @datetime 2023/4/23 18:58
 */
public class InterceptedDataSource extends WrapperAdapter implements DataSource {

    private DataSource dataSource;

    private DataSourceProxy dataSourceProxy;

    public InterceptedDataSource(DataSource dataSource, List<Interceptor> filterList) {
        this.dataSource = dataSource;
        if (filterList == null) {
            throw new IllegalArgumentException("filterList不能为空。");
        }
        dataSourceProxy = new InnerDataSourceProxyImpl(filterList);
    }

    @Override
    public Connection getConnection() throws SQLException {
        InterceptorChain filterChain = new InterceptorChainImpl(dataSourceProxy);
        return filterChain.dataSource_connect(this);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        InterceptorChain filterChain = new InterceptorChainImpl(dataSourceProxy);
        return filterChain.dataSource_connect(this, username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    public ConnectionProxy getConnectionDirect() throws SQLException {
        return new ConnectionProxyImpl(
                dataSourceProxy, dataSource.getConnection(), new Properties(), dataSourceProxy.createConnectionId());
    }

    public ConnectionProxy getConnectionDirect(String username, String password) throws SQLException {
        return new ConnectionProxyImpl(
                dataSourceProxy, dataSource.getConnection(username, password), new Properties(), dataSourceProxy.createConnectionId());
    }

    private static class InnerDataSourceProxyImpl extends DataSourceProxyImpl {

        List<Interceptor> interceptorList;

        List<Filter> filterList;

        public InnerDataSourceProxyImpl(List<Interceptor> interceptorList) {
            super(new Driver() {

                @Override
                public Connection connect(String url, Properties info) throws SQLException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean acceptsURL(String url) throws SQLException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int getMajorVersion() {
                    return 0;
                }

                @Override
                public int getMinorVersion() {
                    return 0;
                }

                @Override
                public boolean jdbcCompliant() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                    throw new UnsupportedOperationException();
                }
            }, new DataSourceProxyConfig());
            this.interceptorList = interceptorList;
            this.filterList = new ArrayList<>(interceptorList);
        }

        @Override
        public List<Filter> getProxyFilters() {
            return filterList;
        }

        @Override
        public String[] getFilterClasses() {
            List<Filter> filterConfigList = getProxyFilters();

            List<String> classes = new ArrayList<>();
            for (Filter filter : filterConfigList) {
                classes.add(filter.getClass().getName());
            }

            return classes.toArray(new String[classes.size()]);
        }

    }

}
