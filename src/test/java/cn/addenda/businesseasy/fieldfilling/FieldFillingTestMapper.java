package cn.addenda.businesseasy.fieldfilling;

import cn.addenda.businesseasy.fieldfilling.annotation.DQLFieldFilling;
import cn.addenda.businesseasy.fieldfilling.annotation.DMLFieldFilling;
import cn.addenda.businesseasy.pojo.TUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ISJINHAO
 * @date 2020/12/11
 */
public interface FieldFillingTestMapper {

    @DQLFieldFilling(allTableNameAvailable = true, independent = true, availableTableNames =
            "T_DISPATCH_FLIGHT_RELEASE,SCORE,RELEASE,ts_role,STUDENT,dual,t1,A,tab2,tab3,tab4,table_listnames")
    List<TUser> selectTest();

    int insertTypeHandlerTest(@Param("tUser") TUser tUser);

    @DMLFieldFilling
    int updateTest();

    @DMLFieldFilling
    int insertTest(@Param("tUser") TUser tUser);

    @DMLFieldFilling
    int deleteTest(@Param("userId") String userId);

}
