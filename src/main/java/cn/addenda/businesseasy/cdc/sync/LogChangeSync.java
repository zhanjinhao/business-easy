package cn.addenda.businesseasy.cdc.sync;

import cn.addenda.businesseasy.cdc.domain.ChangeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @Author ISJINHAO
 * @Date 2022/4/9 21:50
 */
public class LogChangeSync implements ChangeSync {

    private static final Logger logger = LoggerFactory.getLogger(LogChangeSync.class);

    @Override
    public String getName() {
        return "log";
    }

    @Override
    public void sync(List<ChangeEntity> changeEntityList) {
        if (changeEntityList != null) {
            changeEntityList.forEach(item -> logger.info("log : {}. ", item));
        }
    }

}
