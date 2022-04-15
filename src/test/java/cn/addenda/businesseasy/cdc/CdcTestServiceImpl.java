package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.cdc.mapper.CdcTestMapper;
import cn.addenda.businesseasy.pojo.TUser;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @Author ISJINHAO
 * @Date 2022/4/14 18:32
 */
@Component
public class CdcTestServiceImpl implements CdcTestService {

    @Autowired
    private CdcTestMapper cdcTestMapper;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insert() {
        TUser tUser = new TUser();
        tUser.setUserId("cdci20");
        tUser.setUserName("cdcu20");
        tUser.setBirthday(LocalDateTime.now());
        cdcTestMapper.insert(tUser);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchInsert() {

        // 这个方法会将当前 sqlSession 注册到 Spring Tx 上下文中，在事务提交之前，会自动进行 connection.flush，
        // 所以即使 tUser3 之后没有显示调用 flush 语句，依然会将数据写入数据库。
        SqlSession sqlSession = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, null);


        // 使用如下方式 sqlSession 不会注册到 Spring Tx 上下文中。
//        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);

        CdcTestMapper mapper = sqlSession.getMapper(CdcTestMapper.class);

        TUser tUser1 = new TUser();
        tUser1.setUserId("cdcib07");
        tUser1.setBirthday(LocalDateTime.now());
        mapper.insert(tUser1);

        TUser tUser2 = new TUser();
        tUser2.setUserId("cdcib08");
        tUser2.setUserName("cdcub08");
        mapper.insert(tUser2);

        sqlSession.flushStatements();

        TUser tUser3 = new TUser();
        tUser3.setUserId("cdcib09");
        tUser3.setUserName("cdcub09");
        tUser3.setBirthday(LocalDateTime.now());
        mapper.insert(tUser3);

        // 故意不加的，为了测试 下面这段代码
        //     protected void doCommit(DefaultTransactionStatus status) {
        //        List<ChangeEntity> cdcEntityMap = ChangeHolder.getChangeEntityList();
        //        if (cdcEntityMap != null && !cdcEntityMap.isEmpty()) {
        //            Connection connection = retrieveConnectionFromStatus(status);
        //            CdcInterceptor.insertBatchChange(connection, cdcEntityMap);
        //        }
        //        super.doCommit(status);
        //        .....
        //     }
//        sqlSession.commit();

    }

}
