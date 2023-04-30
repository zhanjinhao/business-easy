package cn.addenda.businesseasy.jdbc.dynamiccondition;

import cn.addenda.businesseasy.jdbc.interceptor.dynamiccondition.DynamicConditionContext;
import cn.addenda.businesseasy.jdbc.interceptor.lockingreads.LockingReadsUtils;
import cn.addenda.businesseasy.mapper.TxTest;
import cn.addenda.businesseasy.mapper.TxTestMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

/**
 * @author addenda
 * @datetime 2023/4/27 20:29
 */
public class DynamicConditionTest {

    AnnotationConfigApplicationContext context;

    TxTestMapper txTestMapper;

    @Before
    public void before() {
        context = new AnnotationConfigApplicationContext();
        context.register(DynamicConditionConfiguration.class);

        context.refresh();
        txTestMapper = context.getBean(TxTestMapper.class);
    }

    @Test
    public void test1() {
        DynamicConditionContext.addTableCondition("t_tx_test", "name = 'druid'");
        List<TxTest> txTest = LockingReadsUtils.rSelect(
                () -> {
                    List<TxTest> txTests = txTestMapper.selectAll();
                    System.out.println(txTests);
                    return txTests;
                });
        txTest.forEach(System.out::println);
        DynamicConditionContext.clearConditions();
    }

    @Test
    public void test2() {
        DynamicConditionContext.addViewCondition("t_tx_test", "name = 'druid'");
        List<TxTest> txTest = LockingReadsUtils.wSelect(
                () -> {
                    return txTestMapper.selectAll();
                });
        txTest.forEach(System.out::println);
        DynamicConditionContext.clearConditions();
    }

    @After
    public void after() {
        context.close();
    }

}
