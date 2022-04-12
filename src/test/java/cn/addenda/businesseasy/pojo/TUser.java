package cn.addenda.businesseasy.pojo;

import cn.addenda.businesseasy.fieldfilling.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * @Author ISJINHAO
 * @Date 2022/2/4 16:07
 */
public class TUser extends BaseEntity {

    private String userId;

    private String userName;

    private LocalDateTime birthday;

    public TUser() {
    }

    public TUser(String userId, String userName, LocalDateTime birthday) {
        this.userId = userId;
        this.userName = userName;
        this.birthday = birthday;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDateTime birthday) {
        this.birthday = birthday;
    }

    @Override
    public String toString() {
        return "TUser{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", birthday='" + birthday + '\'' +
                "} " + super.toString();
    }

}
