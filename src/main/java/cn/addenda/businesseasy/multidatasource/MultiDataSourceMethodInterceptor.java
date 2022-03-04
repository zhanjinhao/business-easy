package cn.addenda.businesseasy.multidatasource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * @Author ISJINHAO
 * @Date 2022/3/2 23:03
 */
public class MultiDataSourceMethodInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), invocation.getThis().getClass());
        MultiDataSourceKey multiDataSourceKey = AnnotationUtils.findAnnotation(method, MultiDataSourceKey.class);
        if (multiDataSourceKey != null) {
            MultiDataSource.setCurDataSource(multiDataSourceKey.dataSourceName(), multiDataSourceKey.mode());
        }
        return invocation.proceed();
    }
}
