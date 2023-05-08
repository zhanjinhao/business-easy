package cn.addenda.businesseasy.jdbc.sqlcheck;

import cn.addenda.businesseasy.jdbc.interceptor.sqlcheck.SQLCheckUtils;
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
public class SQLCheckTest {

    AnnotationConfigApplicationContext context;

    TxTestMapper txTestMapper;

    @Before
    public void before() {
        context = new AnnotationConfigApplicationContext();
        context.register(SQLCheckConfiguration.class);

        context.refresh();
        txTestMapper = context.getBean(TxTestMapper.class);
    }

    @Test
    public void test1() {
        txTestMapper.selectAll();
    }

    @Test
    public void test2() {
        txTestMapper.selectJoin();
    }

    @Test
    public void test3() {
        List<TxTest> txTests = SQLCheckUtils.unCheckAllColumn(() -> {
            return txTestMapper.selectAll();
        });
        for (TxTest txTest : txTests) {
            System.out.println(txTest);
        }
    }

    @Test
    public void test4() {
        SQLCheckUtils.unCheckExactIdentifier(() -> {
            return txTestMapper.selectJoin();
        });
    }

    @After
    public void after() {
        context.close();
    }

}
