package cn.addenda.businesseasy.trafficlimit;

/**
 * @author addenda
 * @datetime 2022/12/28 10:26
 */
public interface TrafficLimiter {

    /**
     * @return =0 : 当前请求可以通过；<p/>
     * >0：当前请求能通过，但需要等待指定的时间；<p/>
     * <0: 当前请求不允许通过
     */
    long tryAcquire();

    /**
     * @return true: 当前请求可以通过。在返回true之前可能陷入等待。<p/>
     * false：当前请求不允许通过。在返回false之前不会陷入等待。
     */
    boolean acquire();

}
