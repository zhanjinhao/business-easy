package cn.addenda.businesseasy.multidatasource;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiDataSourceKey {

    String dataSourceName() default MultiDataSourceConstant.DEFAULT;

    String mode() default MultiDataSourceConstant.MASTER;

}
