package cn.addenda.businesseasy.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 12:38
 */
public class BECloneUtil {

    private BECloneUtil() {
        throw new BEUtilException("工具类不可实例化！");
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
            throw new BEUtilException("克隆对象出错！", e);
        }
    }

}
