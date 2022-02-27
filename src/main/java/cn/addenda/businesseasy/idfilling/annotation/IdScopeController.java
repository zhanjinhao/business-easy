package cn.addenda.businesseasy.idfilling.annotation;

import java.lang.annotation.*;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:57
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdScopeController {

    boolean suppress() default true;

    /**
     * false：当前记录的主键属性值不存在时赋值，存在时再赋值
     * true：当前记录的主键属性值即使存在，也要进行赋值
     */
    boolean forceInject() default false;

}
