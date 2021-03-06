package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.cdc.lock.CdcLockManager;
import cn.addenda.businesseasy.cdc.sync.CdcSyncDelegate;
import cn.addenda.businesseasy.cdc.sync.ChangeSync;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author ISJINHAO
 * @Date 2022/4/9 21:48
 */
public class CdcDataSourceTransactionManager extends DataSourceTransactionManager implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

    private CdcLockManager cdcLockManager;

    private List<ChangeSync> changeSyncList;

    private int batchSize = 100;

    private final Set<String> tableNameSet = new HashSet<>();

    private ApplicationContext applicationContext;

    private SqlSessionFactory sqlSessionFactory;

    public CdcDataSourceTransactionManager() {
    }

    public CdcDataSourceTransactionManager(DataSource dataSource) {
        super(dataSource);
    }

    private Connection retrieveConnectionFromStatus(DefaultTransactionStatus status) {
        Object transaction = status.getTransaction();
        if (!"org.springframework.jdbc.datasource.DataSourceTransactionManager$DataSourceTransactionObject".equals(transaction.getClass().getName())) {
            throw new CdcException("无法从 DefaultTransactionStatus 中获取 DataSourceTransactionObject.");
        }
        try {
            Field connectionHolder = JdbcTransactionObjectSupport.class.getDeclaredField("connectionHolder");
            connectionHolder.setAccessible(true);
            return ((ConnectionHolder) connectionHolder.get(transaction)).getConnection();
        } catch (Exception e) {
            throw new CdcException("无法从 DefaultTransactionStatus 中获取 Connection.", e);
        }
    }

//    @Override
//    protected void prepareForCommit(DefaultTransactionStatus status) {
//        super.prepareForCommit(status);
//        // 可能有人不知道在 batch 操作之后需要调用 flushStatements()。
//        // 这里再提交事务之前检测一次，如果线程里存的有就帮它flush。
//        List<ChangeEntity> changeEntityList = ChangeHolder.getChangeEntityList();
//        if (changeEntityList != null && !changeEntityList.isEmpty()) {
//
//            // 进行到这一步 sqlSession 还没释放。
//            // sqlSession 的释放在 org.mybatis.spring.SqlSessionUtils.SqlSessionSynchronization.beforeCompletion。
//            SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sqlSessionFactory);
//            holder.getSqlSession().flushStatements();
//
//            // 这个方法不应该会抛出异常的. so I assume it can execute successfully.
//            Connection connection = retrieveConnectionFromStatus(status);
//
//            try {
//                CdcInterceptor.insertBatchChange(connection, changeEntityList);
//            } catch (Exception e) {
//                try {
//                    connection.rollback();
//                } catch (SQLException ex) {
//                    logger.error("rollback 失败!", ex);
//                }
//                logger.error("事务完成前 batch insert change 失败！", e);
//            }
//        }
//    }

    /**
     * 触发CDC SYNC。
     *
     * @param status
     */
    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        super.doCommit(status);

        // --------------------------------------------------
        // 到这里已经完成了事务，即 dml 及对 dml 的 change 记录都已经完成了。
        // --------------------------------------------------

        // connection 在这里没有被释放。但之前的操作都 commit 了。
        // 所以在这里我们可以在这使用当前 connection。
        DataSource dataSource = getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        if (!CollectionUtils.isEmpty(changeSyncList) && !CollectionUtils.isEmpty(tableNameSet)) {
            CdcSyncDelegate.cdcSync(connection, batchSize, changeSyncList, tableNameSet, cdcLockManager);
        }
    }

    /**
     * 清理掉当前线程里存储的数据。
     *
     * @param transaction
     */
    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        super.doCleanupAfterCompletion(transaction);
        ChangeHolder.remove();
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setChangeSyncList(List<ChangeSync> changeSyncList) {
        this.changeSyncList = changeSyncList;
    }

    public void setTableNameSet(Set<String> tableNameSet) {
        this.tableNameSet.addAll(tableNameSet);
    }

    public void setCdcLockManager(CdcLockManager cdcLockManager) {
        this.cdcLockManager = cdcLockManager;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        DataSource dataSource = getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);

        try {
            if (!CollectionUtils.isEmpty(changeSyncList)) {
                Set<String> syncNameSet = changeSyncList.stream().map(ChangeSync::getName).collect(Collectors.toSet());

                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    throw new CdcException("设置 connection 自动提交为 true 失败！", e);
                }

                CdcSyncDelegate.initSyncName(connection, syncNameSet);
                CdcSyncDelegate.initTableName(connection, tableNameSet, syncNameSet);
            }
        } finally {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException e) {
                logger.error("设置 connection 自动提交为 false 失败！", e);
            }
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        CdcInterceptor.clearTableSet();
        CdcInterceptor.addTableSet(tableNameSet);
        sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
