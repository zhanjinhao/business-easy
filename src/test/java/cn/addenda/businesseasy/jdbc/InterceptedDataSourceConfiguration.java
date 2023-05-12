package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.jdbc.interceptor.InterceptedDataSource;
import cn.addenda.businesseasy.jdbc.interceptor.Interceptor;
import com.alibaba.druid.pool.DruidDataSource;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
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

/**
 * @Author ISJINHAO
 * @Date 2022/2/27 14:03
 */
//@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE)
@PropertySource(value = {"classpath:db.properties"})
public class InterceptedDataSourceConfiguration implements EmbeddedValueResolverAware {

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
        filterList.add(new TestLogInterceptor1());
        filterList.add(new TestLogInterceptor2());
        InterceptedDataSource interceptedDataSource = new InterceptedDataSource(druidDataSource, filterList);

        return interceptedDataSource;
    }

//    @Bean
//    public DataSource dataSource() {
//        HikariDataSource druidDataSource = new HikariDataSource();
//        try {
//            druidDataSource.setDriverClassName(stringValueResolver.resolveStringValue("${db.driver}"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // db.driver=com.alibaba.druid.proxy.DruidDriver
//        // db.url=jdbc:wrap-jdbc:filters=slf4j:jdbc:mysql://foc2dev-m.db-inn.sfcloud.local:3306/foc_auth?
//        // db.username=xxx
//        // db.password=xxx
//        druidDataSource.setJdbcUrl(stringValueResolver.resolveStringValue("${db.url}"));
//        druidDataSource.setUsername(stringValueResolver.resolveStringValue("${db.username}"));
//        druidDataSource.setPassword(stringValueResolver.resolveStringValue("${db.password}"));
//
//        return druidDataSource;
//    }

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
