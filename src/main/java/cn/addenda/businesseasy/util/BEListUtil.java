package cn.addenda.businesseasy.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.util.CollectionUtils;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 12:38
 */
public class BEListUtil {

    private BEListUtil() {
        throw new BEUtilException("工具类不可实例化！");
    }

    /**
     * 集合做拆分
     */
    public static <T> List<List<T>> splitList(List<T> list, int quantity) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        quantity = quantity <= 0 ? list.size() : quantity;
        List<List<T>> splitList = new ArrayList<>();
        int count = 0;
        while (count < list.size()) {
            splitList.add(new ArrayList<>(list.subList(count, Math.min((count + quantity), list.size()))));
            count += quantity;
        }
        return splitList;
    }

}
