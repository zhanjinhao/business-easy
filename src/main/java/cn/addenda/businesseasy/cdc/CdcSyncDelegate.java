package cn.addenda.businesseasy.cdc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Author ISJINHAO
 * @Date 2022/4/10 20:00
 */
public class CdcSyncDelegate {

    private CdcSyncDelegate() {
    }

    private static final Logger logger = LoggerFactory.getLogger(CdcSyncDelegate.class);

    private static final String UPDATE_SYNC_RECORD_NEXT_SQL = "update t_sync_record set next = ?, modify_time = ? where table_name = ? and sync_name = ?";

    private static final String QUERY_CHANGE_SQL = "select id, table_change from t_change_entity where id >= ? and table_name = ? order by id asc limit ?";

    private static final String QUERY_SYNC_RECORD_SQL = "select table_name, sync_name, next from t_sync_record where table_name in (1) and sync_name in (2)";

    private static final String QUERY_SYNC_RECORD_NEXT_SQL = "select next from t_sync_record where table_name = ? and sync_name = ?";

    private static final String INSERT_SYNC_RECORD_SQL = "insert into t_sync_record(table_name, sync_name, next, create_time, modify_time) values (?, ?, ?, ?, ?)";

    private static final String QUERY_ALL_SYNC_NAME_SQL = "select sync_name from t_sync_name";

    private static final String INSERT_SYNC_NAME_SQL = "insert into t_sync_name(sync_name, create_time) values(?, ?)";

    private static long queryNext(Connection connection, String tableName, String syncName) {
        long next = -1;
        try (PreparedStatement preparedStatement = connection.prepareStatement(QUERY_SYNC_RECORD_NEXT_SQL)) {
            preparedStatement.setString(1, tableName);
            preparedStatement.setString(2, syncName);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                next = resultSet.getLong("next");
            } else {
                logger.error("query t_sync_record failed, next not exits. sql : {}, tableName : {}, syncName : {}", QUERY_SYNC_RECORD_NEXT_SQL, tableName, syncName);
            }
        } catch (Exception e) {
            logger.error("query t_sync_record failed, sql exception. sql : {}, tableName : {}, syncName : {}", QUERY_SYNC_RECORD_NEXT_SQL, tableName, syncName, e);
        }
        return next;
    }

    private static List<ChangeEntity> queryChangeEntity(Connection connection, String tableName, long next, int batchSize) {
        List<ChangeEntity> changeEntityList = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(QUERY_CHANGE_SQL)) {
            preparedStatement.setLong(1, next);
            preparedStatement.setString(2, tableName);
            preparedStatement.setLong(3, batchSize);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                String tableChange = resultSet.getString("table_change");
                ChangeEntity changeEntity = new ChangeEntity();
                changeEntity.setId(id);
                changeEntity.setTableChange(tableChange);
                changeEntityList.add(changeEntity);
            }
        } catch (Exception e) {
            logger.error("query t_change_entity failed. sql : {}, tableName : {}", QUERY_CHANGE_SQL, tableName, e);
            changeEntityList.clear();
            return changeEntityList;
        }
        return changeEntityList;
    }

    public static void cdcSync(Connection connection, int batchSize, List<ChangeSync> changeSyncList,
                               Set<String> tableNameSet, CdcLockManager cdcLockManager) {
        for (String tableName : tableNameSet) {
            if (!cdcLockManager.tryLock(tableName)) {
                logger.info("There is another instance is syncing table : {}" + tableName + ", current instance aborts.");
                continue;
            }

            logger.info("start sync, table : {}", tableName);
            try {
                for (ChangeSync changeSync : changeSyncList) {
                    String syncName = changeSync.getName();
                    logger.info("start sync, table : {}, syncName : {}", tableName, syncName);

                    long next = queryNext(connection, tableName, syncName);
                    // 查询 next 失败时，continue 到下一个 sync
                    if (next == -1) {
                        continue;
                    }

                    // now, we get next for <tableName, syncName>.
                    boolean needContinue;
                    do {
                        needContinue = false;
                        List<ChangeEntity> changeEntityList = queryChangeEntity(connection, tableName, next, batchSize);
                        if (!changeEntityList.isEmpty()) {
                            needContinue = (changeEntityList.size() == batchSize);

                            // sync方法抛出了异常不再继续同步
                            try {
                                changeSync.sync(Collections.unmodifiableList(changeEntityList));
                            } catch (Exception e) {
                                logger.error("sync change failed. tableName : {}, syncName : {}, next : {}", tableName, syncName, next, e);
                                break;
                            }

                            // updateNext方法失败不再继续同步
                            next = changeEntityList.get(changeEntityList.size() - 1).getId() + 1;
                            try {
                                updateNext(connection, tableName, syncName, next);
                            } catch (Exception e) {
                                logger.error("update sync record next failed. tableName : {}, syncName : {}, next : {}", tableName, syncName, next, e);
                                break;
                            }
                        }
                    } while (needContinue);

                    logger.info("end sync, table : {}, syncName : {}", tableName, syncName);
                }
            } finally {
                cdcLockManager.releaseLock(tableName);
            }
            logger.info("end sync table : {}", tableName);
        }
    }

    private static void updateNext(Connection connection, String tableName, String syncName, long next) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SYNC_RECORD_NEXT_SQL)) {
            preparedStatement.setLong(1, next);
            preparedStatement.setObject(2, LocalDateTime.now());
            preparedStatement.setString(3, tableName);
            preparedStatement.setString(4, syncName);
            preparedStatement.executeUpdate();
            connection.commit();
        }
    }

    public static void initSyncName(Connection connection, Set<String> configuredSyncNameSet) {
        if (configuredSyncNameSet.isEmpty()) {
            return;
        }

        Set<String> existingSyncNameSet = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(QUERY_ALL_SYNC_NAME_SQL)) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                existingSyncNameSet.add(resultSet.getString("sync_name"));
            }
        } catch (Exception e) {
            throw new CdcException("query t_sync_name 失败！", e);
        }

        for (String configuredSyncName : configuredSyncNameSet) {
            if (!existingSyncNameSet.contains(configuredSyncName)) {
                insertSyncName(configuredSyncName, connection);
            }
        }
    }

    private static void insertSyncName(String configuredSyncName, Connection connection) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SYNC_NAME_SQL)) {
            preparedStatement.setString(1, configuredSyncName);
            preparedStatement.setObject(2, LocalDateTime.now());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new CdcException("insert t_sync_name 失败！", e);
        }
    }

    public static void initTableName(Connection connection, Set<String> configuredTableNameSet, Set<String> configuredSyncNameSet) {
        if (configuredTableNameSet.isEmpty() || configuredSyncNameSet.isEmpty()) {
            return;
        }
        StringBuilder tablesStr = new StringBuilder();
        for (String tableName : configuredTableNameSet) {
            tablesStr.append("'").append(tableName).append("',");
        }
        String querySql = QUERY_SYNC_RECORD_SQL.replace("1", tablesStr.substring(0, tablesStr.length() - 1));

        StringBuilder syncsStr = new StringBuilder();
        for (String syncName : configuredSyncNameSet) {
            syncsStr.append("'").append(syncName).append("',");
        }
        querySql = querySql.replace("2", syncsStr.substring(0, syncsStr.length() - 1));

        Set<SyncRecordEntity> existingTableSync = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(querySql)) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String tableName = resultSet.getString("table_name");
                String syncName = resultSet.getString("sync_name");
                existingTableSync.add(new SyncRecordEntity(tableName, syncName));
            }
        } catch (SQLException e) {
            throw new CdcException("query t_sync_record 失败！", e);
        }

        for (String configuredTableName : configuredTableNameSet) {
            for (String configuredSyncName : configuredSyncNameSet) {
                if (!existingTableSync.contains(new SyncRecordEntity(configuredTableName, configuredSyncName))) {
                    insertTableName(configuredTableName, configuredSyncName, connection);
                }
            }
        }
    }

    private static void insertTableName(String tableName, String syncName, Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_SYNC_RECORD_SQL)) {
            ps.setString(1, tableName);
            ps.setString(2, syncName);
            ps.setLong(3, 0);
            LocalDateTime now = LocalDateTime.now();
            ps.setObject(4, now);
            ps.setObject(5, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new CdcException("insert t_sync_record failed. tableName : " + tableName + ", syncName : " + syncName, e);
        }
    }

}
