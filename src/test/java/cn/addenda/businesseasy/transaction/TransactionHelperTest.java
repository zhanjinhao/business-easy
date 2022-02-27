package cn.addenda.businesseasy.transaction;

import cn.addenda.businesseasy.pojo.TxTest;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author 01395265
 * @date 2022/2/27
 */
public class TransactionHelperTest {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("classpath:cn/addenda/businesseasy/transaction/spring-transactionhelper-context.xml");

        SqlSessionFactory sqlSessionFactory = context.getBean(SqlSessionFactory.class);
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
        TxTestMapper txTestMapper = sqlSession.getMapper(TxTestMapper.class);
        TransactionUtils.doTransaction(Exception.class, () -> {
            txTestMapper.insert(new TxTest("VoidTxExecutor", "123"));
            txTestMapper.insert(new TxTest("VoidTxExecutor", "123"));
            throw new Exception("123");
        });

    }

}
