package cn.addenda.businesseasy.util;

import com.fasterxml.jackson.core.type.TypeReference;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author ISJINHAO
 * @date 2022/2/14
 */
public class BETypeReferenceUtils {

    private BETypeReferenceUtils() {
        throw new BEUtilsException("工具类不可实例化！");
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
            throw new BEUtilsException("An error occurred in getCollectionItemTypeReference(), actualTypeArgument: " + actualTypeArgument, e);
        }
        return typeReference;
    }

    public static <T> TypeReference<T> newTypeReference(Class<T> clazz) {
        TypeReference<Object> typeReference = new TypeReference<Object>() {
        };

        try {
            Field _typeField = typeReferenceClass.getDeclaredField("_type");
            _typeField.setAccessible(true);
            _typeField.set(typeReference, clazz);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new BEUtilsException("An error occurred in newTypeReference(), actualTypeArgument: " + clazz, e);
        }
        return (TypeReference<T>) typeReference;

    }

}
