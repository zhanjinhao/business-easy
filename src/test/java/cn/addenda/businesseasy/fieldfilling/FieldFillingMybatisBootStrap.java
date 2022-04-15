package cn.addenda.businesseasy.fieldfilling;

import cn.addenda.businesseasy.pojo.TUser;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author ISJINHAO
 * @Date 2022/2/3 17:39
 */
public class FieldFillingMybatisBootStrap {

    public static void main(String[] args) {
//        testInsert();
        testBatchInsert();
//        testDelete();
//        testSelect();
//        testInsertTypeHandlerTest();
    }

    private static void testBatchInsert() {
        String resource = "cn/addenda/businesseasy/fieldfilling/mybatis-config-fieldfilling.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);

            try {
                DefaultFieldFillingContext.setCreateUser("addenda");
                DefaultFieldFillingContext.setModifyUser("addenda");
                DefaultFieldFillingContext.setRemark("system");
                FieldFillingTestMapper studentMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
                for (int i = 0; i < 2; i++) {
                    studentMapper.insertTest(new TUser("addenda" + i, "zhanjinhao", LocalDateTime.now()));
                }

                sqlSession.flushStatements();

                for (int i = 2; i < 4; i++) {
                    studentMapper.insertTest(new TUser("addenda" + i, "zhanjinhao", LocalDateTime.now()));
                }

                sqlSession.commit();
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testDelete() {
        String resource = "cn/addenda/businesseasy/fieldfilling/mybatis-config-fieldfilling.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                DefaultFieldFillingContext.setModifyUser("addenda");
                FieldFillingTestMapper studentMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
                studentMapper.deleteTest("addenda");
                sqlSession.commit();
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testInsert() {
        String resource = "cn/addenda/businesseasy/fieldfilling/mybatis-config-fieldfilling.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                DefaultFieldFillingContext.setCreateUser("system");
                DefaultFieldFillingContext.setRemark("system");
                FieldFillingTestMapper studentMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
                for (int i = 6; i < 7; i++) {
                    studentMapper.insertTest(new TUser("addenda" + i, "zhanjinhao", LocalDateTime.now()));
                }
                sqlSession.commit();
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testInsertTypeHandlerTest() {
        String resource = "cn/addenda/businesseasy/fieldfilling/mybatis-config-fieldfilling.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                DefaultFieldFillingContext.setCreateUser("system");
                DefaultFieldFillingContext.setRemark("system");
                FieldFillingTestMapper studentMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
                TUser zhanjinhao = new TUser("addendaTh", "zhanjinhao", LocalDateTime.now());
                zhanjinhao.setCreateTime(LocalDateTime.now().plusYears(-1));
                zhanjinhao.setDelFg((byte) 0);
                studentMapper.insertTypeHandlerTest(zhanjinhao);
                sqlSession.commit();
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testSelect() {
        System.setProperty("businesseasy.timezone", "GMT+8");
        String resource = "cn/addenda/businesseasy/fieldfilling/mybatis-config-fieldfilling.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession();

            try {
                FieldFillingTestMapper studentMapper = sqlSession.getMapper(FieldFillingTestMapper.class);
                List<TUser> tUsers = studentMapper.selectTest();
                tUsers.forEach(System.out::println);
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
