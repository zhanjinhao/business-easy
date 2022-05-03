package cn.addenda.businesseasy.idfilling;

import cn.addenda.businesseasy.fieldfilling.annotation.DMLFieldFilling;
import cn.addenda.businesseasy.pojo.TCourse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:42
 */
public interface IdFillingTestMapper {

    @DMLFieldFilling
    void testInsert(@Param("tCourse") TCourse tCourse);

    @DMLFieldFilling
    void testInsertBatch(@Param("tCourses") List<TCourse> tCourseList);

}
