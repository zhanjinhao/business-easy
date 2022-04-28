package cn.addenda.businesseasy.cdc.apply;

import cn.addenda.businesseasy.cdc.domain.ChangeEntity;

/**
 * @author 01395265
 * @date 2022/4/27
 */
public interface ApplyChangeExecutor {

    int APPLY_SUCCESS = 1;
    int EXCEPTION = -1;
    int HAS_APPLIED = 2;

    /**
     * 这个接口保证幂等性，不保证有序性。
     *
     * @return 1  成功
     * -1  异常
     * -2  未知情景
     */
    int apply(ChangeEntity changeEntity);

}
