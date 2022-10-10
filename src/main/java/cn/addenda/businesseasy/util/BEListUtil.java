package cn.addenda.businesseasy.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.addenda.businesseasy.asynctask.TernaryResult;
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


    public static <T> TernaryResult<List<T>, List<T>, List<T>> separate(List<T> a, List<T> b) {
        a = new ArrayList<>(a);
        b = new ArrayList<>(b);
        List<T> inAButNotInB = new ArrayList<>();
        List<T> inAAndB = new ArrayList<>();

        for (T t : a) {
            Iterator<T> iterator = b.iterator();
            boolean fg = false;
            while (iterator.hasNext()) {
                T next = iterator.next();
                if (t.equals(next)) {
                    inAAndB.add(t);
                    iterator.remove();
                    fg = true;
                    break;
                }
            }
            if (!fg) {
                inAButNotInB.add(t);
            }
        }

        List<T> notInAButInB = new ArrayList<>(b);

        return new TernaryResult<>(inAButNotInB, inAAndB, notInAButInB);
    }

}
