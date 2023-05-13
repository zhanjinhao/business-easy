package cn.addenda.businesseasy.jdbc.dynamicsql;

import cn.addenda.businesseasy.jdbc.interceptor.InterceptedDataSource;
import cn.addenda.businesseasy.jdbc.interceptor.Interceptor;
import cn.addenda.businesseasy.jdbc.interceptor.dynamicsql.DruidDynamicSQLAssembler;
import cn.addenda.businesseasy.jdbc.interceptor.dynamicsql.DynamicSQLInterceptor;
import cn.addenda.businesseasy.jdbc.interceptor.lockingreads.LockingReadsInterceptor;
import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringValueResolver;

import javax.sql.DataSource;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;

/**
 * @author addenda
 * @datetime 2023/4/27 20:30
 */
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE)
@PropertySource(value = {"classpath:db.properties"})
public class DynamicSQLConfiguration implements EmbeddedValueResolverAware {

    private StringValueResolver stringValueResolver;

    @Bean
    public DataSource dataSource() {
        DruidDataSource druidDataSource = new DruidDataSource();
        try {
            druidDataSource.setDriver((Driver) Class.forName(stringValueResolver.resolveStringValue("${db.driver}")).newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
        druidDataSource.setUrl(stringValueResolver.resolveStringValue("${db.url}"));
        druidDataSource.setUsername(stringValueResolver.resolveStringValue("${db.username}"));
        druidDataSource.setPassword(stringValueResolver.resolveStringValue("${db.password}"));
        druidDataSource.setMaxActive(1);
        druidDataSource.setInitialSize(1);

        List<Interceptor> filterList = new ArrayList<>();
        filterList.add(new LockingReadsInterceptor());
        filterList.add(new DynamicSQLInterceptor(new DruidDynamicSQLAssembler(false)));
        return new InterceptedDataSource(druidDataSource, filterList);
    }

    @Bean
    public TransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setBasePackage("cn.addenda.businesseasy.mapper");
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return mapperScannerConfigurer;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(new ClassPathResource("cn/addenda/businesseasy/mapper/TxTestMapper.xml"));
        return sqlSessionFactoryBean.getObject();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.stringValueResolver = resolver;
    }

}
