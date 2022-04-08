package cn.addenda.businesseasy.idfilling;

/**
 * @Author ISJINHAO
 * @Date 2022/2/4 14:38
 */
public class NanoTimeIdGenerator implements IdGenerator {

    @Override
    public String nextSqc(String scopeName) {
        return scopeName + ":" + System.nanoTime();
    }

}
