package cn.addenda.businesseasy.helper;

import org.apache.ibatis.annotations.Param;

/**
 * @author addenda
 * @datetime 2023/3/8 15:26
 */
public interface UserMapper {

    void insertConsumer(User user);

    Integer insertFunction(User user);

    void updateConsumer(@Param("prefix") String prefix);

    Integer updateFunction(@Param("prefix") String prefix);

    void deleteConsumer(@Param("prefix") String prefix);

    void deleteFunction(@Param("prefix") String prefix);

}
