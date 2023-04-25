package cn.addenda.businesseasy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.Test;

/**
 * @author addenda
 * @datetime 2022/11/17 19:18
 */
public class BeListUtilTest {

    @Test
    public void test1() {
        BEListUtils.acceptInBatches(BEArrayUtils.asArrayList("a", "b"), System.out::println, 1);
    }

    @Test
    public void test2() {
        BEListUtils.applyInBatches(BEArrayUtils.asArrayList("a", "b"), new Function<List<String>, List<Void>>() {
            @Override
            public List<Void> apply(List<String> objects) {
                System.out.println(objects);
                return null;
            }
        }, 1);
    }


    @Test
    public void test3() {
        BEListUtils.acceptInBatches(
            BEArrayUtils.asArrayList("a", "b"),
            BEArrayUtils.asArrayList(1, 2),
            (objects, objects2) -> System.out.println(objects.toString() + objects2.toString()), 1);
    }

    @Test
    public void test4() {
        List<String> list = BEListUtils.applyInBatches(
            BEArrayUtils.asArrayList("a", "b"),
            BEArrayUtils.asArrayList(1, 2),
            (objects, objects2) -> {
                return new ArrayList<>(Collections.singletonList(objects.toString() + objects2.toString()));
            }, 1);
        list.forEach(System.out::println);
    }

}
