package cn.addenda.businesseasy.idfilling;

import java.util.UUID;

/**
 * @Author ISJINHAO
 * @Date 2022/2/4 14:38
 */
public class UUIDIdGenerator implements IdGenerator {

    @Override
    public String nextSqc(String scopeName) {
        return scopeName + ":" + UUID.randomUUID();
    }

}
