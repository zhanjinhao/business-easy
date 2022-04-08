package cn.addenda.businesseasy.resulthandler;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author ISJINHAO
 * @Date 2022/2/7 16:38
 */
public class MapResultHandler<T> implements ResultHandler<Map<T, T>> {

    // 用LinkedHashMap保证有序
    private final Map<T, T> resultMap = new LinkedHashMap<>();

    private final boolean repeatedValid;

    private final boolean fifo;

    private final boolean trimNull;

    public static final String KEY_REP = "key";
    public static final String VALUE_REP = "value";

    public MapResultHandler(boolean repeatedValid, boolean fifo, boolean trimNull) {
        this.repeatedValid = repeatedValid;
        this.fifo = fifo;
        this.trimNull = trimNull;
    }

    public MapResultHandler(boolean fifo, boolean trimNull) {
        this(true, fifo, trimNull);
    }

    public MapResultHandler(boolean repeatedValid) {
        this.repeatedValid = repeatedValid;
        this.fifo = true;
        this.trimNull = true;
    }

    @Override
    public void handleResult(ResultContext<? extends Map<T, T>> resultContext) {
        Map<T, T> resultObject = resultContext.getResultObject();
        // key 和 value 都为null的时候，resultObject为null
        if (resultObject == null) {
            return;
        }
        // key 不能为null，value 可以为null
        T key = resultObject.get(KEY_REP);
        if (key == null && trimNull) {
            return;
        }
        T value = resultObject.get(VALUE_REP);

        if (!resultMap.containsKey(key)) {
            resultMap.put(key, value);
        } else {
            if (repeatedValid) {
                if (fifo) {
                    // non-op
                } else {
                    resultMap.put(key, value);
                }
            } else {
                throw new ResultHelperException("出现了重复的key, key: " + key);
            }
        }

    }

    public Map<T, T> getResult() {
        return resultMap;
    }

}
