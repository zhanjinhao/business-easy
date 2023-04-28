package cn.addenda.businesseasy.jdbc.lockingreads;

import cn.addenda.businesseasy.jdbc.interceptor.lockingreads.LockingReadsUtils;
import cn.addenda.businesseasy.mapper.TxTest;
import cn.addenda.businesseasy.mapper.TxTestMapper;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author addenda
 * @datetime 2023/4/27 20:29
 */
public class LockingReadsTest {

    AnnotationConfigApplicationContext context;

    TxTestMapper txTestMapper;

    @Before
    public void before() {
        context = new AnnotationConfigApplicationContext();
        context.register(LockingReadsConfiguration.class);

        context.refresh();
        txTestMapper = context.getBean(TxTestMapper.class);
    }

    @Test
    public void test1() {
        List<TxTest> txTest = LockingReadsUtils.rSelect(
            () -> {
                List<TxTest> txTests = txTestMapper.selectAll();
                System.out.println(txTests);
                return txTests;
            });
        txTest.forEach(System.out::println);
    }

    @Test
    public void test2() {
        List<TxTest> txTest = LockingReadsUtils.wSelect(
            () -> {
                return txTestMapper.selectAll();
            });
        txTest.forEach(System.out::println);
    }

    @Test
    public void test3() {
        List<TxTest> txTest = txTestMapper.selectAll();
        txTest.forEach(System.out::println);
    }

    @After
    public void after() {
        context.close();
    }

}
