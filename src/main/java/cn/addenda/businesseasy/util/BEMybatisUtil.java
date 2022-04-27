package cn.addenda.businesseasy.util;

import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.SimpleExecutor;

import java.lang.reflect.Field;

/**
 * @Author ISJINHAO
 * @Date 2022/4/15 18:28
 */
public class BEMybatisUtil {

    private BEMybatisUtil() {
    }

    public static boolean isSimpleExecutor(Executor executor) {
        if (executor instanceof SimpleExecutor) {
            return true;
        }
        if (executor instanceof BatchExecutor) {
            return false;
        }
        if (executor instanceof CachingExecutor) {
            try {
                Field delegate = CachingExecutor.class.getDeclaredField("delegate");
                delegate.setAccessible(true);
                Object o = delegate.get(executor);
                return isSimpleExecutor((Executor) o);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new BEUtilException("无法从 CachingExecutor 中获取到 delegate。当前Executor: " + executor.getClass() + ".", e);
            }
        }
        throw new BEUtilException("只支持 SimpleExecutor 和 BatchExecutor! 当前是：" + executor.getClass() + ".");
    }

}
