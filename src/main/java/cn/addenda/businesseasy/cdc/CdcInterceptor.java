package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.cdc.domain.ChangeEntity;
import cn.addenda.businesseasy.util.BEMybatisUtil;
import cn.addenda.businesseasy.util.BESqlUtil;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Author ISJINHAO
 * @Date 2022/4/8 19:07
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "flushStatements", args = {})
})
public class CdcInterceptor implements Interceptor {

    private static final Set<String> TABLE_SET = new HashSet<>();

    private static final String INSERT_ENTITY_SQL = "insert into t_change_entity(table_name, table_change, create_time) values (?, ?, ?)";

    /**
     * 无论是 invocation.proceed() 还是 cdc，出了异常就抛出。当前事务进行 rollback。
     *
     * @param invocation
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object proceed = invocation.proceed();

        Method method = invocation.getMethod();
        String name = method.getName();

        if ("update".equals(name)) {
            cdcUpdateIfNecessary(invocation);
        }
        // 执行 flushStatements 时将存在线程里 change 入库
        else if ("flushStatements".equals(name)) {
            cdcFlushStatementsIfNecessary(invocation);
        }
        return proceed;
    }


    private void cdcUpdateIfNecessary(Invocation invocation) {

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];

        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        if (!SqlCommandType.INSERT.equals(sqlCommandType) &&
                !SqlCommandType.UPDATE.equals(sqlCommandType) && !SqlCommandType.DELETE.equals(sqlCommandType)) {
            return;
        }

        BoundSql boundSql = ms.getBoundSql(args[1]);
        String sql = boundSql.getSql();

        String tableName = BESqlUtil.extractDmlTableName(sql);
        if (!TABLE_SET.contains(tableName)) {
            return;
        }

        // 不走 interceptor ...
        ParameterHandler parameterHandler = ms.getLang().createParameterHandler(ms, boundSql.getParameterObject(), boundSql);
        ParameterPreparedStatement parameterPreparedStatement = new ParameterPreparedStatement(sql);
        try {
            parameterHandler.setParameters(parameterPreparedStatement);
        } catch (Exception e) {
            throw new CdcException("生成明文SQL时出错！", e);
        }
        sql = parameterPreparedStatement.getPlainSql();

        // do not format sql here.
        // I want to remove the blank char in sql, use:  `sql = sql.replaceAll("\\s+", " ")`
        // but it will create error, for example : insert into t_user(name) values ('da  ci').
        // If I use sql parser to analyze the sql, the time cost will be expensive.

        Executor executor = (Executor) invocation.getTarget();
        ChangeEntity changeEntity = new ChangeEntity(tableName, sql, LocalDateTime.now());
        if (BEMybatisUtil.isSimpleExecutor(executor)) {
            // SimpleExecutor 场景下，在 update 时 insert change。
            Connection connection = retrieveConnection(executor);
            try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_ENTITY_SQL)) {
                preparedStatement.setString(1, changeEntity.getTableName());
                preparedStatement.setString(2, changeEntity.getTableChange());
                preparedStatement.setObject(3, changeEntity.getCreateTime());
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new CdcException("insert change 失败！sql: " + sql + ", entity: " + changeEntity + ".", e);
            }
        } else {
            // 对于 BatchExecutor，将 change 存在当前线程里面。
            ChangeHolder.addCdcEntity(changeEntity);
        }

    }

    /**
     * 在BatchExecutor执行update的时候，不能进行change记录的insert，会影响jdbc的batch行为。
     * 执行到这个方法时，BatchExecutor里面存的数据已经被清空了，所以可以使用当前Connection了。
     *
     * @param invocation
     */
    private void cdcFlushStatementsIfNecessary(Invocation invocation) {
        List<ChangeEntity> changeEntityList = ChangeHolder.getChangeEntityList();
        if (changeEntityList == null || changeEntityList.isEmpty()) {
            return;
        }

        Executor executor = (Executor) invocation.getTarget();
        Connection connection = retrieveConnection(executor);

        insertBatchChange(connection, changeEntityList);
    }

    public static void insertBatchChange(Connection connection, List<ChangeEntity> changeEntityList) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_ENTITY_SQL)) {
            for (ChangeEntity changeEntity : changeEntityList) {
                preparedStatement.setString(1, changeEntity.getTableName());
                preparedStatement.setString(2, changeEntity.getTableChange());
                preparedStatement.setObject(3, changeEntity.getCreateTime());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            // change 写入完成之后就不清理掉，减少内存占用。
            ChangeHolder.remove();
        } catch (SQLException e) {
            throw new CdcException("insert change 失败！sql: " + INSERT_ENTITY_SQL + ", changeEntityList: " + changeEntityList + ".", e);
        }
    }

    private Connection retrieveConnection(Executor executor) {
        try {
            return executor.getTransaction().getConnection();
        } catch (Exception e) {
            throw new CdcException("无法从 Executor 里获取 Connection.", e);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    public static void clearTableSet() {
        TABLE_SET.clear();
    }

    public static void addTableSet(Set<String> tableSet) {
        TABLE_SET.addAll(tableSet);
    }

    @Override
    public void setProperties(Properties properties) {
        String tableSet = (String) properties.get("tableSet");
        if (tableSet != null) {
            TABLE_SET.addAll(Arrays.asList(tableSet.split(";")));
        }
    }
}
