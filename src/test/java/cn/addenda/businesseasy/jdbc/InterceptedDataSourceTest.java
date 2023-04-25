package cn.addenda.businesseasy.jdbc;

import cn.addenda.businesseasy.mapper.TxTest;
import cn.addenda.businesseasy.mapper.TxTestMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author addenda
 * @datetime 2023/4/22 21:25
 */
public class InterceptedDataSourceTest {
    AnnotationConfigApplicationContext context;

    TxTestMapper txTestMapper;

    @Before
    public void before() {
        context = new AnnotationConfigApplicationContext();
        context.register(InterceptedDataSourceConfiguration.class);

        context.refresh();
        txTestMapper = context.getBean(TxTestMapper.class);
    }

    @Test
    public void test1() {
        txTestMapper.insert(new TxTest("druid", "interceptor"));
    }

    @After
    public void after() {
        context.close();
    }

}
