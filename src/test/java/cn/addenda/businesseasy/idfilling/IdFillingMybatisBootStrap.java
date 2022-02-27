package cn.addenda.businesseasy.idfilling;

import cn.addenda.businesseasy.fieldfilling.DefaultFieldFillingContext;
import cn.addenda.businesseasy.pojo.TCourse;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 17:39
 */
public class IdFillingMybatisBootStrap {

    public static void main(String[] args) {
//        testInsertBatch();
        testInsert();
    }

    private static void testInsert() {
        String resource = "cn/addenda/businesseasy/idfilling/mybatis-config-idfilling.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                DefaultFieldFillingContext.setCreateUser("system");
                DefaultFieldFillingContext.setRemark("system");
                IdFillingTestMapper courseMapper = sqlSession.getMapper(IdFillingTestMapper.class);
                courseMapper.testInsert(new TCourse("addenda", "zhanjinhao"));
                courseMapper.testInsert(new TCourse(null, "zhanjinhao"));
                sqlSession.commit();
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void testInsertBatch() {
        String resource = "cn/addenda/businesseasy/idfilling/mybatis-config-idfilling.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                DefaultFieldFillingContext.setCreateUser("system");
                DefaultFieldFillingContext.setRemark("system");
                IdFillingTestMapper courseMapper = sqlSession.getMapper(IdFillingTestMapper.class);
                List<TCourse> list = new ArrayList<>();
                list.add(new TCourse(null, "zhanjinhao"));
                list.add(new TCourse(null, "zhanjinhao"));
                courseMapper.testInsertBatch(list);
                sqlSession.commit();
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
