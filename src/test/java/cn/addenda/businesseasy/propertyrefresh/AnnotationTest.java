package cn.addenda.businesseasy.propertyrefresh;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * @Author ISJINHAO
 * @Date 2022/4/4 11:27
 */
public class AnnotationTest {

    private static final Class<? extends Annotation> propertyRefreshClass = PropertyRefresh.class;

    public static void main(String[] args) {

        ReflectionUtils.doWithFields(BusinessServiceImpl.class, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                Annotation[] annotations = field.getAnnotations();
                for (Annotation annotation : annotations) {
                    if (propertyRefreshClass.equals(annotation.annotationType())) {
                        System.out.println("aaa");
                    }
                }
            }
        });

    }

}
