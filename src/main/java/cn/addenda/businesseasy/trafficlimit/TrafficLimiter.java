package cn.addenda.businesseasy.trafficlimit;

/**
 * @author addenda
 * @datetime 2022/12/28 10:26
 */
public interface TrafficLimiter {

    /**
     * 当前请求能否满足
     */
    boolean tryAcquire();
}
