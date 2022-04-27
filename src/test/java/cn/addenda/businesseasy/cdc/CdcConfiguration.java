package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.cdc.lock.MonolithicCdcLockManager;
import cn.addenda.businesseasy.cdc.sync.ChangeSync;
import cn.addenda.businesseasy.cdc.sync.LogChangeSync;
import cn.addenda.businesseasy.multidatasource.MultiDataSource;
import cn.addenda.businesseasy.multidatasource.MultiDataSourceConstant;
import cn.addenda.businesseasy.multidatasource.MultiDataSourceEntry;
import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringValueResolver;

import javax.sql.DataSource;
import java.sql.Driver;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @Author ISJINHAO
 * @Date 2022/4/14 18:23
 */
@EnableTransactionManagement
@PropertySource(value = {"classpath:db.properties"})
@Configuration
public class CdcConfiguration implements EmbeddedValueResolverAware {

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

        MultiDataSourceEntry multiDataSourceEntry = new MultiDataSourceEntry();
        multiDataSourceEntry.setMaster(druidDataSource);

        MultiDataSource multiDataSource = new MultiDataSource();
        multiDataSource.addMultiDataSourceEntry(MultiDataSourceConstant.DEFAULT, multiDataSourceEntry);
        return multiDataSource;
    }

    @Order(1)
    @Bean
    public ChangeSync logChangeSync() {
        return new LogChangeSync();
    }

    @Order(2)
    @Bean
    public ChangeSync log2ChangeSync() {
        return new Log2ChangeSync();
    }

    @Bean
    public TransactionManager transactionManager(DataSource dataSource, List<ChangeSync> changeSyncList) {
        CdcDataSourceTransactionManager cdcDataSourceTransactionManager = new CdcDataSourceTransactionManager(dataSource);
        cdcDataSourceTransactionManager.setBatchSize(2);
        cdcDataSourceTransactionManager.setTableNameSet(new HashSet<>(Arrays.asList("t_user", "t_course")));
        cdcDataSourceTransactionManager.setChangeSyncList(changeSyncList);
        cdcDataSourceTransactionManager.setCdcLockManager(new MonolithicCdcLockManager());
        return cdcDataSourceTransactionManager;
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setBasePackage("cn.addenda.businesseasy.cdc.mapper");
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return mapperScannerConfigurer;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(new ClassPathResource("cn/addenda/businesseasy/cdc/CdcTestMapper.xml"));
        sqlSessionFactoryBean.setPlugins(new CdcInterceptor());
        return sqlSessionFactoryBean.getObject();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.stringValueResolver = resolver;
    }

}
