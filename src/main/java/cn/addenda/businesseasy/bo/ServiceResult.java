package cn.addenda.businesseasy.bo;

import java.io.Serializable;

/**
 * @Author ISJINHAO
 * @Date 2022/2/11 11:40
 */
public class ServiceResult<T> implements Serializable {

    // 调用方需要判断这个值，看看下一步怎么处理
    private SRStatus srStatus = SRStatus.SUCCESS;

    private String errorMsg;

    private long startTm = System.currentTimeMillis();
    private long endTm = System.currentTimeMillis();

    private T result;

    public ServiceResult(SRStatus srStatus, T result) {
        this.srStatus = srStatus;
        this.result = result;
    }

    public ServiceResult(T result) {
        this.result = result;
    }

    public ServiceResult() {
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public SRStatus getSrStatus() {
        return srStatus;
    }

    public void setSrStatus(SRStatus srStatus) {
        this.srStatus = srStatus;
    }

    public void setStartTm(long startTm) {
        this.startTm = startTm;
    }

    public long getStartTm() {
        return startTm;
    }

    public void setEndTm(long endTm) {
        this.endTm = endTm;
    }

    public long getEndTm() {
        return endTm;
    }
}
