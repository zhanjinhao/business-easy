package cn.addenda.businesseasy.util;

import com.fasterxml.jackson.core.type.TypeReference;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * @author 01395265
 * @date 2022/2/14
 */
public class BETypeReferenceUtil {

    private BETypeReferenceUtil() {
        throw new BEUtilException("工具类不可实例化！");
    }

    private static final Class<?> collectionClass = java.util.Collection.class;
    private static final Class<?> typeReferenceClass = TypeReference.class;

    public static <T> TypeReference<T> getCollectionItemTypeReference(TypeReference<T> typeReference) {
        Type superClass = typeReference.getClass().getGenericSuperclass();
        if (superClass instanceof Class<?>) {
            throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
        }
        ParameterizedTypeImpl actualTypeArgument = (ParameterizedTypeImpl) ((ParameterizedType) superClass).getActualTypeArguments()[0];
        try {
            if (!actualTypeArgument.getRawType().isAssignableFrom(collectionClass)) {
                return typeReference;
            }
            Class<?> itemClass = (Class<?>) actualTypeArgument.getActualTypeArguments()[0];
            Field _typeField = typeReferenceClass.getDeclaredField("_type");
            _typeField.setAccessible(true);
            _typeField.set(typeReference, itemClass);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new BEUtilException("An error occurred in getCollectionItemTypeReference(), actualTypeArgument: " + actualTypeArgument, e);
        }
        return typeReference;
    }

}
