package cn.addenda.businesseasy.mapper;


import java.util.List;

/**
 * @author ISJINHAO
 * @date 2020/7/27
 */
public interface TxTestMapper {

    Integer insert(TxTest txTest);

    List<TxTest> selectAll();

}
