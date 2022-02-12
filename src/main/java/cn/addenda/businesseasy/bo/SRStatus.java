package cn.addenda.businesseasy.bo;

/**
 * @Author ISJINHAO
 * @Date 2022/2/11 11:43
 */
public enum SRStatus {

    // 成功
    SUCCESS,

    // 发生错误，异常被处理
    ERROR,

    // 业务逻辑失败，但无异常
    FAILED,

    // 状态ErrorMsg定，需要调用方判断ErrorMsg
    DISPATCH

}
