package cn.addenda.businesseasy.transaction;

import cn.addenda.businesseasy.util.BEUtilException;
import java.lang.reflect.Method;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionAttribute;

public class TransactionHelper extends TransactionAspectSupport {

    public TransactionHelper() {
        setTransactionAttributeSource(new TransactionHelperAttributeSource());
    }

    /**
     * 最简单的事务控制场景（当发生任何异常（Exception.class）都回滚事务），
     */
    public <T> T doTransaction(TransactionExecutor<T> executor) {
        return doTransaction(Exception.class, executor);
    }


    /**
     * 较上一个场景，该场景可以指定针对特定的异常类型发生事务回滚
     */
    public <T> T doTransaction(Class<? extends Throwable> rollbackFor, TransactionExecutor<T> executor) {
        TransactionAttribute transactionAttribute = TransactionAttributeBuilder.newBuilder().rollbackFor(rollbackFor).build();
        return doTransaction(transactionAttribute, executor);
    }

    /**
     * 最复杂的场景，需要手动指定所有的事务控制参数。
     */
    public <T> T doTransaction(TransactionAttribute txAttr, TransactionExecutor<T> executor) {
        return _process(txAttr, executor);
    }

    private <T> T _process(TransactionAttribute txAttr, TransactionExecutor<T> executor) {
        TransactionHelperAttributeSource.setTransactionAttribute(txAttr);
        try {
            return (T) invokeWithinTransaction(extractMethod(executor), executor.getClass(), new InvocationCallback() {
                @Override
                public Object proceedWithInvocation() throws Throwable {
                    return executor.process();
                }
            });
        } catch (Throwable e) {
            throw new BEUtilException("事务在TransactionHelper内执行失败！", e);
        }
    }

    private <T> Method extractMethod(TransactionExecutor<T> executor) {
        Method[] methods = executor.getClass().getMethods();
        for (Method method : methods) {
            if ("process".equals(method.getName()) && method.getParameterCount() == 0) {
                return method;
            }
        }
        throw new BEUtilException("找不到 TransactionExecutor#process() 方法。");
    }

    public interface TransactionExecutor<T> {

        T process();
    }

}
