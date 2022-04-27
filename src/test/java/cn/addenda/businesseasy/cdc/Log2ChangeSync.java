package cn.addenda.businesseasy.cdc;

import cn.addenda.businesseasy.cdc.domain.ChangeEntity;
import cn.addenda.businesseasy.cdc.sync.ChangeSync;
import cn.addenda.businesseasy.cdc.sync.LogChangeSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Log2ChangeSync implements ChangeSync {

    private static final Logger logger = LoggerFactory.getLogger(LogChangeSync.class);

    @Override
    public String getName() {
        return "log2";
    }

    @Override
    public void sync(List<ChangeEntity> changeEntityList) {
        if (changeEntityList != null) {
            changeEntityList.forEach(item -> {
//                if (item.getId() > 20) {
//                    throw new CdcException("test log2 exception! ");
//                }
                logger.info("log2  --->  " + item.toString());
            });
        }
    }

}
