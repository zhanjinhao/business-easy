package cn.addenda.businesseasy.jdbc.baseentity;

import cn.addenda.businesseasy.jdbc.interceptor.baseentity.BaseEntity;
import cn.addenda.businesseasy.jdbc.interceptor.baseentity.DefaultBaseEntitySource;

/**
 * @author addenda
 * @since 2023/5/3 17:13
 */
public class DefaultBaseEntitySourceTest {

    public static void main(String[] args) {
        DefaultBaseEntitySource defaultBaseEntityContext = new DefaultBaseEntitySource();
        System.out.println(defaultBaseEntityContext.get(BaseEntity.N_CREATOR));
        System.out.println(defaultBaseEntityContext.get(BaseEntity.N_CREATOR_NAME));
        System.out.println(defaultBaseEntityContext.get(BaseEntity.N_CREATE_TIME));
        System.out.println(defaultBaseEntityContext.get(BaseEntity.N_MODIFIER));
        System.out.println(defaultBaseEntityContext.get(BaseEntity.N_MODIFIER_NAME));
        System.out.println(defaultBaseEntityContext.get(BaseEntity.N_MODIFY_TIME));
        DefaultBaseEntitySource.setRemark("defaultBaseEntitySource1");
        System.out.println(defaultBaseEntityContext.get(BaseEntity.N_REMARK));
        System.out.println(defaultBaseEntityContext.get(BaseEntity.N_REMARK));

        DefaultBaseEntitySource.execute("defaultBaseEntitySource2", () -> {
            System.out.println(defaultBaseEntityContext.get(BaseEntity.N_REMARK));
        });

        System.out.println(defaultBaseEntityContext.get(BaseEntity.N_REMARK));
    }

}
