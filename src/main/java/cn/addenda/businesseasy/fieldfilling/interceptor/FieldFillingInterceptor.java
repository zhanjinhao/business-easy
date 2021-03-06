package cn.addenda.businesseasy.fieldfilling.interceptor;

import cn.addenda.businesseasy.fieldfilling.FieldFillingContext;
import cn.addenda.businesseasy.fieldfilling.FiledFillingException;
import cn.addenda.businesseasy.fieldfilling.annotation.FieldFillingForReading;
import cn.addenda.businesseasy.fieldfilling.annotation.FieldFillingForWriting;
import cn.addenda.businesseasy.util.BEMybatisUtil;
import cn.addenda.businesseasy.util.BESqlUtil;
import cn.addenda.businesseasy.util.AnnotationUtil;
import cn.addenda.ro.grammar.ast.statement.Curd;
import cn.addenda.ro.grammar.ast.statement.Literal;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 17:25
 */
@Intercepts({
    @Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
    @Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "flushStatements", args = {}),
    @Signature(type = Executor.class, method = "commit", args = {boolean.class})
})
public class FieldFillingInterceptor implements Interceptor {

    private static final String DEFAULT_FIELD_FILLING_CONTEXT_NAME = "defaultFieldFillingContext";

    private FieldFillingContext defaultFieldFillingContext;

    private final Set<String> globalAvailableTableNameSet = new HashSet<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Method method = invocation.getMethod();
        String name = method.getName();
        if ("flushStatements".equals(name) || "commit".equals(name)) {
            Object proceed = invocation.proceed();
            defaultFieldFillingContext.removeCache();
            return proceed;
        }

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameterObject = args[1];

        String msId = ms.getId();

        BoundSql boundSql = ms.getBoundSql(parameterObject);
        String oldSql = boundSql.getSql();

        Executor executor = (Executor) invocation.getTarget();

        String newSql = processSql(oldSql, ms, BEMybatisUtil.isSimpleExecutor(executor));

        if (!oldSql.replaceAll("\\s+", "").equals(newSql.replaceAll("\\s+", ""))) {
            resetSql2Invocation(invocation, newSql);
        }
        return invocation.proceed();
    }

    private String processSql(String oldSql, MappedStatement ms, boolean clearCache) {
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        String msId = ms.getId();
        if (SqlCommandType.SELECT.equals(sqlCommandType)) {
            FieldFillingForReading fieldFillingForReading = extractAnnotation(msId, FieldFillingForReading.class);
            return processSelect(oldSql, fieldFillingForReading);
        } else if (SqlCommandType.INSERT.equals(sqlCommandType)) {
            FieldFillingForWriting fieldFillingForWriting = extractAnnotation(msId, FieldFillingForWriting.class);
            return processInsert(oldSql, fieldFillingForWriting, clearCache);
        } else if (SqlCommandType.UPDATE.equals(sqlCommandType)) {
            FieldFillingForWriting fieldFillingForWriting = extractAnnotation(msId, FieldFillingForWriting.class);
            return processUpdate(oldSql, fieldFillingForWriting, clearCache);
        } else if (SqlCommandType.DELETE.equals(sqlCommandType)) {
            FieldFillingForWriting fieldFillingForWriting = extractAnnotation(msId, FieldFillingForWriting.class);
            return processDelete(oldSql, fieldFillingForWriting, clearCache);
        } else {
            throw new FiledFillingException("???????????????Mybatis SqlCommandType???" + sqlCommandType);
        }
    }

    private <T> T extractAnnotation(String msId, Class<T> tClazz) {
        int end = msId.lastIndexOf(".");
        try {
            Class<?> aClass = Class.forName(msId.substring(0, end));
            String methodName = msId.substring(end + 1);
            return AnnotationUtil.extractAnnotationFromMethod(aClass, methodName, tClazz);
        } catch (ClassNotFoundException e) {
            throw new FiledFillingException("?????????????????????Mapper???" + msId, e);
        }
    }

    private String processDelete(String sql, FieldFillingForWriting fieldFillingForWriting, boolean clearCache) {
        if (fieldFillingForWriting == null) {
            return sql;
        }
        String deleteLogically = BESqlUtil.deleteLogically(sql, null, null);
        return processUpdate(deleteLogically, fieldFillingForWriting, clearCache);
    }

    private String processUpdate(String sql, FieldFillingForWriting fieldFillingForWriting, boolean clearCache) {
        if (fieldFillingForWriting == null) {
            return sql;
        }
        String fillingContextClazzName = fieldFillingForWriting.fieldFillingContextClazzName();
        FieldFillingContext fieldFillingContext = getFieldFillingContext(fillingContextClazzName);
        Map<String, Curd> entryMap = new LinkedHashMap<>();
        String modifyUser = fieldFillingContext.getModifyUser();
        if (modifyUser != null) {
            entryMap.put("modify_user", new Literal(new Token(TokenType.STRING, modifyUser)));
        } else {
            entryMap.put("modify_user", new Literal(new Token(TokenType.NULL, "null")));
        }
        entryMap.put("modify_time", new Literal(new Token(TokenType.NUMBER, fieldFillingContext.getModifyTime())));
        String remark = fieldFillingContext.getRemark();
        if (remark != null) {
            entryMap.put("remark", new Literal(new Token(TokenType.STRING, remark)));
        } else {
            entryMap.put("remark", new Literal(new Token(TokenType.NULL, "null")));
        }
        if (clearCache) {
            fieldFillingContext.removeCache();
        }
        return BESqlUtil.updateAddEntry(sql, entryMap);
    }

    private String processInsert(String sql, FieldFillingForWriting fieldFillingForWriting, boolean clearCache) {
        if (fieldFillingForWriting == null) {
            return sql;
        }
        String fillingContextClazzName = fieldFillingForWriting.fieldFillingContextClazzName();
        FieldFillingContext fieldFillingContext = getFieldFillingContext(fillingContextClazzName);
        Map<String, Curd> entryMap = new LinkedHashMap<>();
        String createUser = fieldFillingContext.getCreateUser();
        if (createUser != null) {
            entryMap.put("create_user", new Literal(new Token(TokenType.STRING, fieldFillingContext.getCreateUser())));
        } else {
            entryMap.put("create_user", new Literal(new Token(TokenType.NULL, "null")));
        }
        entryMap.put("create_time", new Literal(new Token(TokenType.NUMBER, fieldFillingContext.getCreateTime())));
        entryMap.put("del_fg", new Literal(new Token(TokenType.NUMBER, 0)));
        String remark = fieldFillingContext.getRemark();
        if (remark != null) {
            entryMap.put("remark", new Literal(new Token(TokenType.STRING, remark)));
        } else {
            entryMap.put("remark", new Literal(new Token(TokenType.NULL, "null")));
        }
        if (clearCache) {
            fieldFillingContext.removeCache();
        }
        return BESqlUtil.insertAddEntry(sql, entryMap);
    }

    private String processSelect(String sql, FieldFillingForReading fieldFillingForReading) {
        if (fieldFillingForReading == null) {
            return sql;
        }
        if (fieldFillingForReading.allTableNameAvailable()) {
            return BESqlUtil.selectAddComparison(sql, null);
        }

        String availableTableNames = fieldFillingForReading.availableTableNames();
        boolean independent = fieldFillingForReading.independent();
        if (independent) {
            if (availableTableNames == null || availableTableNames.length() == 0) {
                return BESqlUtil.selectAddComparison(sql, null, new HashSet<>());
            }
            Set<String> collect = Arrays.stream(availableTableNames.split(",")).collect(Collectors.toSet());
            return BESqlUtil.selectAddComparison(sql, null, collect);
        } else {
            if (availableTableNames == null || availableTableNames.length() == 0) {
                return BESqlUtil.selectAddComparison(sql, null, globalAvailableTableNameSet);
            }
            Set<String> collect = Arrays.stream(availableTableNames.split(",")).collect(Collectors.toSet());
            globalAvailableTableNameSet.addAll(collect);
            return BESqlUtil.selectAddComparison(sql, null, globalAvailableTableNameSet);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        if (properties.containsKey(DEFAULT_FIELD_FILLING_CONTEXT_NAME)) {
            String defaultFieldFillingContextNameValue = (String) properties.get(DEFAULT_FIELD_FILLING_CONTEXT_NAME);
            if (defaultFieldFillingContext != null) {
                this.defaultFieldFillingContext = newInstance(defaultFieldFillingContextNameValue);
            }
        }
    }

    private FieldFillingContext newInstance(String clazzName) {
        try {
            Class<?> aClass = Class.forName(clazzName);
            if (!FieldFillingContext.class.isAssignableFrom(aClass)) {
                throw new FiledFillingException("FieldFillingContext??????????????????" + clazzName + "?????????cn.addenda.daoeasy.fieldfilling.FieldFillingContext?????????!");
            }
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("getInstance") && Modifier.isStatic(method.getModifiers()) &&
                    method.getParameterCount() == 0 && FieldFillingContext.class.isAssignableFrom(method.getReturnType())) {
                    return (FieldFillingContext) method.invoke(null);
                }
            }
            return (FieldFillingContext) aClass.newInstance();
        } catch (Exception e) {
            throw new FiledFillingException("FieldFillingContext??????????????????" + clazzName, e);
        }
    }

    /**
     * ????????????????????????????????????????????????
     */
    private FieldFillingContext getFieldFillingContext(String clazzName) {
        if (clazzName == null || clazzName.length() == 0) {
            return defaultFieldFillingContext;
        }
        return newInstance(clazzName);
    }

    private static class BoundSqlSqlSource implements SqlSource {

        private final BoundSql boundSql;

        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }

    private void resetSql2Invocation(Invocation invocation, String sql) throws SQLException {
        final Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement) args[0];
        Object parameterObject = args[1];
        BoundSql boundSql = statement.getBoundSql(parameterObject);
        MappedStatement newStatement = newMappedStatement(statement, new BoundSqlSqlSource(boundSql));
        MetaObject msObject = SystemMetaObject.forObject(newStatement);
        msObject.setValue("sqlSource.boundSql.sql", sql);
        args[0] = newStatement;

        // ?????????????????????6?????????????????? BoundSql ??????
        if (6 == args.length) {
            BoundSql boundSqlArg = (BoundSql) args[5];
            // ????????????????????????sql?????????set???????????????????????????????????????
            Class<? extends BoundSql> aClass = boundSql.getClass();
            try {
                Field field = aClass.getDeclaredField("sql");
                field.setAccessible(true);
                field.set(boundSqlArg, sql);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private MappedStatement newMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder =
            new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
            StringBuilder keyProperties = new StringBuilder();
            for (String keyProperty : ms.getKeyProperties()) {
                keyProperties.append(keyProperty).append(",");
            }
            keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
            builder.keyProperty(keyProperties.toString());
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

}
