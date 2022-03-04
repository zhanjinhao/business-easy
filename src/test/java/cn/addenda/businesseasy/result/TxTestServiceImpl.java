package cn.addenda.businesseasy.result;

import cn.addenda.businesseasy.multidatasource.MultiDataSourceConstant;
import cn.addenda.businesseasy.multidatasource.MultiDataSourceKey;
import cn.addenda.businesseasy.result.mapper.TxTestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author ISJINHAO
 * @Date 2022/2/26 23:01
 */
public class TxTestServiceImpl implements TxTestService {

    @Autowired
    private TxTestMapper txTestMapper;

    @Override
    @MultiDataSourceKey(mode = MultiDataSourceConstant.SLAVE)
    @ServiceResultConvertible(errorToSuccess = true, errorToFailed = false, exceptionClass = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public ServiceResult<Boolean> insert(TxTest txTest) {
        boolean success = txTestMapper.insert(txTest) > 0;
        return new ServiceResult<>(ServiceResultStatus.SUCCESS, success);
    }

}
