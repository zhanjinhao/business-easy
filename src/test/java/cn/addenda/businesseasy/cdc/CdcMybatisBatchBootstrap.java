package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.cdc.mapper.CdcTestMapper;
import cn.addenda.businesseasy.pojo.TUser;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;

/**
 * @Author ISJINHAO
 * @Date 2022/4/9 10:57
 */
public class CdcMybatisBatchBootstrap {

    public static void main(String[] args) {
        batchTest();
    }

    private static void batchTest() {
        String resource = "cn/addenda/businesseasy/cdc/mybatis-config-cdc.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);

            try {

                CdcTestMapper mapper = sqlSession.getMapper(CdcTestMapper.class);

                TUser tUser1 = new TUser();
                tUser1.setUserId("cdci1");
                tUser1.setBirthday(LocalDateTime.now());
                mapper.insert(tUser1);

                TUser tUser2 = new TUser();
                tUser2.setUserId("cdci2");
                tUser2.setUserName("cdcu2");
                mapper.insert(tUser2);

                sqlSession.flushStatements();

                TUser tUser3 = new TUser();
                tUser3.setUserId("cdci3");
                tUser3.setUserName("cdcu3");
                tUser3.setBirthday(LocalDateTime.now());
                mapper.insert(tUser3);

                sqlSession.flushStatements();

                sqlSession.commit();
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
