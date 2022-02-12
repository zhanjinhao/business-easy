package cn.addenda.businesseasy.bo;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConvertSRStatus {

    // 哪些error能强制转换为failed
    Class<? extends Throwable> exceptionClass() default RuntimeException.class;

    boolean errorToFailed() default true;

    boolean errorToSuccess() default false;

    String[] errorToDispatch() default {"java.lang.RuntimeException:inner exception occurred!"};

}
