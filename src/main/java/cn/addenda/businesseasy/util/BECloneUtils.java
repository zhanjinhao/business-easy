package cn.addenda.businesseasy.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 12:38
 */
public class BECloneUtils {

    private BECloneUtils() {
        throw new BEUtilsException("工具类不可实例化！");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T cloneByJDKSerialization(T obj) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bout);
            oos.writeObject(obj);
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new BEUtilsException("克隆对象出错！", e);
        }
    }

    public static <T extends Serializable> Collection<T> cloneByJDKSerialization(Collection<T> obj) {
        if (obj == null) {
            return null;
        }
        Collection<T> newList = newInstance((Class<Collection<T>>) obj.getClass());
        if (obj.isEmpty()) {
            return newList;
        }
        for (T next : obj) {
            newList.add(cloneByJDKSerialization(next));
        }
        return newList;
    }

    private static <T extends Serializable> Collection<T> newInstance(Class<Collection<T>> collection) {
        try {
            return collection.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BEUtilsException("反射生成集合对象失败！", e);
        }
    }

}
