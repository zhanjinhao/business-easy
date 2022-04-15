package cn.addenda.businesseasy.cdc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author ISJINHAO
 * @Date 2022/4/9 22:42
 */
public class ChangeHolder {

    private ChangeHolder() {
    }

    private static final Logger logger = LoggerFactory.getLogger(ChangeHolder.class);

    private static final ThreadLocal<List<ChangeEntity>> BATCH_CHANGE_ENTITY_LOCAL = new ThreadLocal<>();

    public static List<ChangeEntity> getChangeEntityList() {
        return BATCH_CHANGE_ENTITY_LOCAL.get();
    }

    public static void addCdcEntity(ChangeEntity changeEntity) {
        List<ChangeEntity> stringListMap = BATCH_CHANGE_ENTITY_LOCAL.get();
        if (stringListMap == null) {
            stringListMap = new ArrayList<>();
            BATCH_CHANGE_ENTITY_LOCAL.set(stringListMap);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("add change to BATCH_CHANGE_ENTITY_LOCAL : {}", changeEntity);
        }
        stringListMap.add(changeEntity);
    }

    public static void remove() {
        BATCH_CHANGE_ENTITY_LOCAL.remove();
    }

}
