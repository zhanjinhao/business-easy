package cn.addenda.businesseasy.result.mapper;

import cn.addenda.businesseasy.multidatasource.MultiDataSourceConstant;
import cn.addenda.businesseasy.multidatasource.MultiDataSourceKey;
import cn.addenda.businesseasy.result.TxTest;

/**
 * @author 01395265
 * @date 2020/7/27
 */
public interface TxTestMapper {

    @MultiDataSourceKey(mode = MultiDataSourceConstant.SLAVE)
    Integer insert(TxTest txTest);

}
