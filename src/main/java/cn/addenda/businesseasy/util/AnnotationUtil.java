package cn.addenda.businesseasy.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 16:20
 */
public class AnnotationUtil {

    public static <T> T extractAnnotationFromMethod(Class<?> aClass, String methodName, Class<T> tClazz) {
        Method[] methods = aClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Annotation[] methodAnnotations = method.getAnnotations();
                for (Annotation annotation : methodAnnotations) {
                    if (tClazz.isAssignableFrom(annotation.getClass())) {
                        return (T) annotation;
                    }
                }
            }
        }
        return null;
    }

    public static <T> T extractAnnotationFromClass(Class<?> aClass, Class<T> tClazz) {
        Annotation[] annotations = aClass.getAnnotations();
        for (Annotation annotation : annotations) {
            if (tClazz.isAssignableFrom(annotation.getClass())) {
                return (T) annotation;
            }
        }
        return null;
    }

}
