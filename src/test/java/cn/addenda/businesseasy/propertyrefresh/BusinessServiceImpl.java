package cn.addenda.businesseasy.propertyrefresh;

import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * @Author ISJINHAO
 * @Date 2022/4/4 11:28
 */
public class BusinessServiceImpl implements BusinessService, PropertyRefreshListener {

    @Value("#{'${addressList}'.split(',')}")
    @PropertyRefresh
    private List<String> addressList;

    @Override
    public void doPropertyRefresh(PropertyRefreshHolder holder) {
        System.out.println(holder);
    }

}
