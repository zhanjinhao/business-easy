package cn.addenda.businesseasy.resulthandler;

/**
 * @Author ISJINHAO
 * @Date 2022/2/5 15:42
 */
public interface MapResultHandlerTestMapper {

    void testStringMapHandler(MapResultHandler<String> resultHelper);

    void testLongMapHandler(MapResultHandler<Long> resultHelper);

}
