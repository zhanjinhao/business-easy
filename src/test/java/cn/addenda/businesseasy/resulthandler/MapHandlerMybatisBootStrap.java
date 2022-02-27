package cn.addenda.businesseasy.resulthandler;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 17:39
 */
public class MapHandlerMybatisBootStrap {

    public static void main(String[] args) {
//        testString();
        testLong();
    }

    private static void testString() {
        String resource = "cn/addenda/businesseasy/resulthandler/mybatis-config-maphandler.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                MapResultHandlerTestMapper courseMapper = sqlSession.getMapper(MapResultHandlerTestMapper.class);
                MapResultHandler<String> resultHelper = new MapResultHandler<>(true, true);
                courseMapper.testStringMapHandler(resultHelper);
                System.out.println(resultHelper.getResult());
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testLong() {
        String resource = "cn/addenda/businesseasy/resulthandler/mybatis-config-maphandler.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                MapResultHandlerTestMapper courseMapper = sqlSession.getMapper(MapResultHandlerTestMapper.class);
                MapResultHandler<Long> resultHelper = new MapResultHandler<>(false, false);
                courseMapper.testLongMapHandler(resultHelper);
                System.out.println(resultHelper.getResult());
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
