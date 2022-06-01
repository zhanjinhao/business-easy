package cn.addenda.businesseasy.asynctask;

import org.springframework.beans.BeanUtils;

/**
 * @author 01395265
 * @date 2022/5/31
 */
public interface Result<T extends Result<T>> {

    default T merge(Object abstractResult) {
        BeanUtils.copyProperties(abstractResult, this);
        return (T) this;
    }

}
