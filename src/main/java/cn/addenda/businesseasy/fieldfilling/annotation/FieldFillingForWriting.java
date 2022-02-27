package cn.addenda.businesseasy.fieldfilling.annotation;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldFillingForWriting {

    String fieldFillingContextClazzName() default "";

}
