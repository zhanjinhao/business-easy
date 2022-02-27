package cn.addenda.businesseasy.idfilling.interceptor;

import cn.addenda.businesseasy.idfilling.IdFillingException;
import cn.addenda.businesseasy.idfilling.IdGenerator;
import cn.addenda.businesseasy.idfilling.annotation.IdScope;
import cn.addenda.businesseasy.idfilling.annotation.IdScopeController;
import cn.addenda.businesseasy.util.AnnotationUtil;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 20:17
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class IdFillingInterceptor implements Interceptor {

    private static final String idGeneratorName = "idGenerator";

    private IdGenerator idGenerator;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameterObject = args[1];

        String msId = ms.getId();

        // IdScopeController可以压制注入ID
        IdScopeController idScopeController = extractAnnotation(msId);
        if (idScopeController != null && idScopeController.suppress()) {
            return invocation.proceed();
        }

        if (parameterObject instanceof Collection) {
            injectCollection((Collection<?>) parameterObject, idScopeController);
        } else if (parameterObject instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) parameterObject;
            Set<? extends Map.Entry<?, ?>> entries = map.entrySet();
            for (Map.Entry<?, ?> next : entries) {
                Object value = next.getValue();
                if (value instanceof Collection) {
                    injectCollection((Collection<?>) value, idScopeController);
                } else {
                    injectPojo(value, idScopeController);
                }
            }
        } else {
            injectPojo(parameterObject, idScopeController);
        }
        return invocation.proceed();
    }

    private void injectCollection(Collection<?> collection, IdScopeController idScopeController) {
        IdScope idScope = extractIdScopeFromCollection(collection);
        if (idScope != null) {
            for (Object parameter : collection) {
                injectPojo(parameter, idScopeController);
            }
        }
    }

    private void injectPojo(Object object, IdScopeController idScopeController) {
        IdScope idScope = extractIdScopeFromObject(object);

        // 当idScopeController明确forceInject为true时，才会强制赋值
        boolean b = idScopeController != null && idScopeController.forceInject();
        MetaObject metaObject = SystemMetaObject.forObject(object);
        if (b) {
            metaObject.setValue(idScope.idFieldName(), idGenerator.nextSqc(idScope.scopeName()));
        } else {
            Object value = metaObject.getValue(idScope.idFieldName());
            if (value == null) {
                metaObject.setValue(idScope.idFieldName(), idGenerator.nextSqc(idScope.scopeName()));
            }
        }
    }

    private IdScope extractIdScopeFromCollection(Collection<?> collection) {
        // 如果参数是Collection且集合为空，不注入
        if (collection == null || collection.isEmpty()) {
            return null;
        }

        Iterator<?> iterator = collection.iterator();
        if (iterator.hasNext()) {
            return extractIdScopeFromObject(iterator.next());
        }
        // 理论上走不到这一步，因为已经对集合进行判空了
        else {
            return null;
        }
    }


    private IdScope extractIdScopeFromObject(Object object) {
        if (object == null) {
            return null;
        }
        return AnnotationUtil.extractAnnotationFromClass(object.getClass(), IdScope.class);
    }


    @Override
    public void setProperties(Properties properties) {
        if (properties.containsKey(idGeneratorName)) {
            String defaultFieldFillingContext = (String) properties.get(idGeneratorName);
            if (defaultFieldFillingContext != null) {
                idGenerator = newInstance(defaultFieldFillingContext);
            }
        }
    }


    private IdGenerator newInstance(String clazzName) {
        try {
            Class<?> aClass = Class.forName(clazzName);
            if (!IdGenerator.class.isAssignableFrom(aClass)) {
                throw new IdFillingException("IdFillingContext初始化失败：" + clazzName + "需要是cn.addenda.daoeasy.idfilling.IdFillingContext的子类!");
            }
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("getInstance") && Modifier.isStatic(method.getModifiers()) &&
                        method.getParameterCount() == 0 && IdGenerator.class.isAssignableFrom(method.getReturnType())) {
                    return (IdGenerator) method.invoke(null);
                }
            }
            return (IdGenerator) aClass.newInstance();
        } catch (Exception e) {
            throw new IdFillingException("FieldFillingContext初始化失败：" + clazzName, e);
        }
    }


    private IdScopeController extractAnnotation(String msId) {
        int end = msId.lastIndexOf(".");
        try {
            Class<?> aClass = Class.forName(msId.substring(0, end));
            String methodName = msId.substring(end + 1);
            return AnnotationUtil.extractAnnotationFromMethod(aClass, methodName, IdScopeController.class);
        } catch (ClassNotFoundException e) {
            throw new IdFillingException("无法找到对应的Mapper：" + msId, e);
        }
    }

}
