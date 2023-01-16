package cn.addenda.businesseasy.trafficlimit;

/**
 * @author addenda
 * @datetime 2022/12/28 14:14
 */
public class SlidingLogTrafficLimiterTest {

    public static void main(String[] args) throws Exception {
        SlidingLogTrafficLimiter slidingLogTrafficLimiter = new SlidingLogTrafficLimiter(10, 1000);
        new TrafficLimiterBaseTest(slidingLogTrafficLimiter).test(true);
    }

}
