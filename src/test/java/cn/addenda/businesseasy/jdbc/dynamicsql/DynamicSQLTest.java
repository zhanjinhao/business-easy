package cn.addenda.businesseasy.jdbc.dynamicsql;

import cn.addenda.businesseasy.jdbc.interceptor.dynamicsql.DynamicSQLContext;
import cn.addenda.businesseasy.jdbc.interceptor.lockingreads.LockingReadsUtils;
import cn.addenda.businesseasy.mapper.TxTest;
import cn.addenda.businesseasy.mapper.TxTestMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @author addenda
 * @datetime 2023/4/27 20:29
 */
public class DynamicSQLTest {

    AnnotationConfigApplicationContext context;

    TxTestMapper txTestMapper;

    @Before
    public void before() {
        context = new AnnotationConfigApplicationContext();
        context.register(DynamicSQLConfiguration.class);

        context.refresh();
        txTestMapper = context.getBean(TxTestMapper.class);
    }

    @Test
    public void test1() {
        DynamicSQLContext.tableAddJoinCondition("t_tx_test", "name = 'druid1'");
        DynamicSQLContext.viewAddJoinCondition("t_tx_test", "name = 'druid2'");
        DynamicSQLContext.tableAddWhereCondition("t_tx_test", "name = 'druid3'");
        DynamicSQLContext.viewAddWhereCondition("t_tx_test", "name = 'druid4'");
        List<TxTest> txTest = LockingReadsUtils.rSelect(
                () -> {
                    List<TxTest> txTests = txTestMapper.selectAll();
                    System.out.println(txTests);
                    return txTests;
                });
        txTest.forEach(System.out::println);
        DynamicSQLContext.clearCondition();
    }

    @Test
    public void test2() {
        DynamicSQLContext.insertAddItem("t_tx_test", "test_date", LocalDate.now());
        DynamicSQLContext.insertAddItem("t_tx_test", "test_time", LocalTime.now());
        TxTest txTest = new TxTest();
        txTest.setName("insertAddItem1");
        txTest.setRemark("insertAddItem2");
        txTestMapper.insert(txTest);
        DynamicSQLContext.clearItem();
    }


    @Test
    public void test3() {
        DynamicSQLContext.updateAddItem("t_tx_test", "test_date", LocalDate.now());
        DynamicSQLContext.updateAddItem("t_tx_test", "test_time", LocalTime.now());
        TxTest txTest = new TxTest();
        txTest.setName("insertAddItem1");
        txTest.setRemark("insertAddItem2");
        txTestMapper.updateByName(txTest);
        DynamicSQLContext.clearItem();
    }

    @After
    public void after() {
        context.close();
    }

}
