package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import java.lang.annotation.*;

/**
 * @author addenda
 * @since 2023/5/2 23:11
 */
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateField {
}
