package cn.addenda.businesseasy.jdbc.interceptor.baseentity;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * @author addenda
 * @since 2023/5/2 19:36
 */
public interface BaseEntitySource {

    String getCreator();

    String getCreatorName();

    Object getCreateTime();

    String getModifier();

    String getModifierName();

    Object getModifyTime();

    String getRemark();

    default Object get(String fieldName) {
        // todo methodinvoke 可以提升性能？
        Method method = ReflectionUtils.findMethod(this.getClass(),
                "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
        return ReflectionUtils.invokeMethod(method, this);
    }

}
