package cn.addenda.businesseasy.fieldfilling;

import cn.addenda.businesseasy.fieldfilling.annotation.FieldFillingForReading;
import cn.addenda.businesseasy.fieldfilling.annotation.FieldFillingForWriting;
import cn.addenda.businesseasy.pojo.TUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 01395265
 * @date 2020/12/11
 */
public interface FieldFillingTestMapper {

    @FieldFillingForReading(allTableNameAvailableFg = true, independent = true, availableTableNames =
            "T_DISPATCH_FLIGHT_RELEASE,SCORE,RELEASE,ts_role,STUDENT,dual,t1,A,tab2,tab3,tab4,table_listnames")
    List<TUser> selectTest();

    int insertTypeHandlerTest(@Param("tUser") TUser tUser);

    @FieldFillingForWriting
    int updateTest();

    @FieldFillingForWriting
    int insertTest(@Param("tUser") TUser tUser);

    @FieldFillingForWriting
    int deleteTest(@Param("userId") String userId);

}
