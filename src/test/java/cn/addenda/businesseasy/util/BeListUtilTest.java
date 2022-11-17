package cn.addenda.businesseasy.util;

import org.junit.Test;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author addenda
 * @datetime 2022/11/17 19:18
 */
public class BeListUtilTest {

    @Test
    public void test1() {
        BEListUtil.inList(BEArrayUtil.asArrayList("a", "b"),
                new Function<List<String>, List<Void>>() {
                    @Override
                    public List<Void> apply(List<String> objects) {
                        System.out.println(objects);
                        return null;
                    }
                }, 1);
    }


    @Test
    public void test2() {
        BEListUtil.inList(BEArrayUtil.asArrayList("a", "b"), BEArrayUtil.asArrayList(1, 2),
                (BiFunction<List<String>, List<Integer>, List<Void>>) (objects, objects2) -> {
                    System.out.println(objects);
                    System.out.println(objects2);
                    return null;
                }, 1);
    }

}
