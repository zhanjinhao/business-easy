package cn.addenda.businesseasy.cdc.apply;

import cn.addenda.businesseasy.cdc.CdcException;
import cn.addenda.businesseasy.cdc.domain.ChangeEntity;
import cn.addenda.businesseasy.util.BEConnectionUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 01395265
 * @date 2022/4/28
 */
@Slf4j
public class DataSourceApplyChangeExecutor implements ApplyChangeExecutor {

    private DataSource dataSource;

    private static final String QUERY_BY_APPLIED_CHANGE_ID = "select count(*) as count from t_change_apply where applied_change_id = ? for update";

    private static final String INSERT_CHANGE_APPLY = "insert into t_change_apply(applied_change_id) values(?)";

    @Override
    public int apply(ChangeEntity changeEntity) {
        if (dataSource == null) {
            throw new CdcException("当前 DataSourceApplyChangeExecutor 没有配置 DataSource.");
        }

        // get connection from datasource.
        Connection connection = BEConnectionUtil.openConnection(dataSource);
        if (connection == null) {
            return EXCEPTION;
        }

        // get current autoCommit.
        int autoCommit = BEConnectionUtil.setAutoCommit(connection, false);
        if (autoCommit == -1) {
            BEConnectionUtil.close(connection);
            return EXCEPTION;
        }

        // get current transactionIsolation.
        int transactionIsolation = BEConnectionUtil.setTransactionIsolation(connection, Connection.TRANSACTION_REPEATABLE_READ);
        if (transactionIsolation == -1) {
            BEConnectionUtil.setAutoCommit(connection, autoCommit == 1);
            BEConnectionUtil.close(connection);
            return EXCEPTION;
        }

        // query whether the change has been processed.
        // transaction begin here.
        try (PreparedStatement ps = connection.prepareStatement(QUERY_BY_APPLIED_CHANGE_ID)) {
            ps.setLong(1, changeEntity.getId());
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                long count = resultSet.getLong("count");
                if (count > 0) {
                    BEConnectionUtil.setAutoCommit(connection, autoCommit == 1);
                    BEConnectionUtil.setTransactionIsolation(connection, transactionIsolation);
                    BEConnectionUtil.close(connection);
                    return HAS_APPLIED;
                }
            }
        } catch (SQLException | CdcException e) {
            BEConnectionUtil.setAutoCommit(connection, autoCommit == 1);
            BEConnectionUtil.setTransactionIsolation(connection, transactionIsolation);
            BEConnectionUtil.close(connection);
            if (e instanceof SQLException) {
                log.error("按变更ID查询变更是否执行 失败！dataSource: {}，change: {}", dataSource, changeEntity, e);
                return EXCEPTION;
            } else {
                throw (CdcException) e;
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(changeEntity.getTableChange());
        } catch (SQLException e) {
            log.error("执行变更出错！changeEntity: {} .", changeEntity, e);
            BEConnectionUtil.rollback(connection);
            BEConnectionUtil.setAutoCommit(connection, autoCommit == 1);
            BEConnectionUtil.setTransactionIsolation(connection, transactionIsolation);
            BEConnectionUtil.close(connection);
            return EXCEPTION;
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_CHANGE_APPLY)) {
            preparedStatement.setLong(1, changeEntity.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("记录变更出错！changeEntity: {} .", changeEntity, e);
            BEConnectionUtil.rollback(connection);
            BEConnectionUtil.setAutoCommit(connection, autoCommit == 1);
            BEConnectionUtil.setTransactionIsolation(connection, transactionIsolation);
            BEConnectionUtil.close(connection);
            return EXCEPTION;
        }

        try {
            return BEConnectionUtil.commit(connection) ? APPLY_SUCCESS : EXCEPTION;
        } finally {
            BEConnectionUtil.setAutoCommit(connection, autoCommit == 1);
            BEConnectionUtil.setTransactionIsolation(connection, transactionIsolation);
            BEConnectionUtil.close(connection);
        }
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

}
