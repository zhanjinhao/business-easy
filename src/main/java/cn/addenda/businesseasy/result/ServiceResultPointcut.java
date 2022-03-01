package cn.addenda.businesseasy.result;

import cn.addenda.businesseasy.util.BEUtilException;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * @Author ISJINHAO
 * @Date 2022/2/27 22:25
 */
public class ServiceResultPointcut extends StaticMethodMatcherPointcut {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        Method actualMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        ServiceResultConvertible annotation = AnnotationUtils.findAnnotation(actualMethod, ServiceResultConvertible.class);
        if (annotation != null) {
            Class<?> returnType = actualMethod.getReturnType();
            if (ServiceResult.class.isAssignableFrom(returnType)) {
                if (annotation.errorToSuccess() ^ annotation.errorToFailed()) {
                    return true;
                } else {
                    throw new BEUtilException("errorToSuccess ^ errorToFailed 需要为 true");
                }
            } else {
                throw new BEUtilException("标注 ServiceResultConvertible 注解的方法的返回值需要为 cn.addenda.businesseasy.result.ServiceResult");
            }
        }

        return false;
    }

}
