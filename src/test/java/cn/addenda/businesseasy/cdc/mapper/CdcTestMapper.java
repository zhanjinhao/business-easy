package cn.addenda.businesseasy.cdc.mapper;

import cn.addenda.businesseasy.pojo.TUser;
import org.apache.ibatis.annotations.Param;

/**
 * @author ISJINHAO
 * @date 2020/12/11
 */
public interface CdcTestMapper {

    int insert(@Param("tUser") TUser tUser);

}
