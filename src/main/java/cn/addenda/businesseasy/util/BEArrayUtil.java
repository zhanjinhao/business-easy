package cn.addenda.businesseasy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 12:37
 */
public class BEArrayUtil {

    private BEArrayUtil() {
        throw new BEUtilException("工具类不可实例化！");
    }

    public static <T> List<T> asList(T... objs) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, objs);
        return list;
    }

}
