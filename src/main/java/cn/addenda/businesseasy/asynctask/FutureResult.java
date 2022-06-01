package cn.addenda.businesseasy.asynctask;

import java.util.ArrayList;
import java.util.List;
import lombok.ToString;

/**
 * @author 01395265
 * @date 2022/5/24
 */
@ToString
public class FutureResult<T> {


    private T result;

    private List<Throwable> throwableList = new ArrayList<>();

    public FutureResult() {
    }

    public FutureResult(List<Throwable> throwable) {
        this.throwableList.addAll(throwable);
    }

    public FutureResult(T result) {
        this.result = result;
    }

    public boolean isSuccess() {
        return throwableList.isEmpty();
    }

    public T getResult() {
        return result;
    }


    public void setResult(T result) {
        this.result = result;
    }

    public List<Throwable> getThrowableList() {
        return throwableList;
    }

    public static <U> FutureResult<U> combineThrowableList(FutureResult<?>... futureResults) {
        List<Throwable> list = new ArrayList<>();
        for (FutureResult<?> futureResult : futureResults) {
            list.addAll(futureResult.getThrowableList());
        }
        return new FutureResult<>(list);
    }

    public <U> FutureResult<U> convertTypeWithThrowableList(Class<U> clazz) {
        return new FutureResult<>(this.getThrowableList());
    }

    public void reportError() {
        for (Throwable throwable : throwableList) {
            throw new AsyncTaskException(throwable);
        }
    }

    public T getResultWithThrowFirstThrowable() {
        if (!isSuccess()) {
            reportError();
        }
        return result;
    }

}
