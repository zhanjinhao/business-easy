package cn.addenda.businesseasy.pojo;

import cn.addenda.businesseasy.fieldfilling.entity.BaseEntity;

/**
 * @Author ISJINHAO
 * @Date 2022/2/4 16:07
 */
public class TUser extends BaseEntity {

    private String userId;

    private String userName;

    public TUser() {
    }

    public TUser(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
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

    @Override
    public String toString() {
        return "TUser{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                "} " + super.toString();
    }

}
