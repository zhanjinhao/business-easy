package cn.addenda.businesseasy.transaction;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author 01395265
 * @description TODO
 * @date 2022/2/27
 */
public class TransactionHelperTest {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("");

        SqlSessionFactory sqlSessionFactory = context.getBean(SqlSessionFactory.class);
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);

    }

}
