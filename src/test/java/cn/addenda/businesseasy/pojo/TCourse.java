package cn.addenda.businesseasy.pojo;

import cn.addenda.businesseasy.fieldfilling.entity.BaseEntity;
import cn.addenda.businesseasy.idfilling.annotation.IdScope;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:28
 */
@IdScope(scopeName = "TCourse", idFieldName = "courseId")
public class TCourse extends BaseEntity {

    private String courseId;

    private String courseName;

    public TCourse() {
    }

    public TCourse(String courseId, String courseName) {
        this.courseId = courseId;
        this.courseName = courseName;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    @Override
    public String toString() {
        return "TCourse{" +
                "courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                "} " + super.toString();
    }
}
