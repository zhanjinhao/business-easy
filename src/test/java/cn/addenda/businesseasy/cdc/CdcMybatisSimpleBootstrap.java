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
public class CdcMybatisSimpleBootstrap {

    public static void main(String[] args) {
        batchTest();
    }

    private static void batchTest() {
        String resource = "cn/addenda/businesseasy/cdc/mybatis-config-cdc.xml";
        Reader reader;
        try {
            reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);

            try {
                CdcTestMapper mapper = sqlSession.getMapper(CdcTestMapper.class);
                TUser tUser4 = new TUser();
                tUser4.setUserId("cdci5");
                tUser4.setBirthday(LocalDateTime.now());
                System.out.println(mapper.insert(tUser4));

                sqlSession.commit();
            } finally {
                sqlSession.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
