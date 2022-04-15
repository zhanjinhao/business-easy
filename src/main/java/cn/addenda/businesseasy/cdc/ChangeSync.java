package cn.addenda.businesseasy.cdc;

import java.util.List;

public interface ChangeSync {

    String getName();

    /**
     * 建议此方法实现时业务逻辑在另一个线程里执行，避免业务线程 timeOut。
     * 在很多场景下，都需要保证CDC是有序的，所以多线程时务必注意这点。
     *
     * @param changeEntityList
     */
    void sync(List<ChangeEntity> changeEntityList);

}
