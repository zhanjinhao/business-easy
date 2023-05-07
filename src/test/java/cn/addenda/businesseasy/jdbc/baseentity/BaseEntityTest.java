package cn.addenda.businesseasy.jdbc.baseentity;

import cn.addenda.businesseasy.jdbc.interceptor.baseentity.BaseEntity;

/**
 * @author addenda
 * @since 2023/5/3 17:09
 */
public class BaseEntityTest {

    public static void main(String[] args) {
        System.out.println(BaseEntity.getUpdateFieldNameList());
        System.out.println(BaseEntity.getUpdateColumnNameList());
        System.out.println(BaseEntity.getAllFieldNameList());
        System.out.println(BaseEntity.getAllColumnNameList());
    }

}
