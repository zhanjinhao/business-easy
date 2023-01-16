package cn.addenda.businesseasy.trafficlimit;

import org.junit.Test;

/**
 * @author addenda
 * @datetime 2022/12/28 17:13
 */
public class RequestIntervalTrafficLimiterTest {

    @Test
    public void test1() throws Exception {
        RequestIntervalTrafficLimiter requestIntervalTrafficLimiter = new RequestIntervalTrafficLimiter(10d);
        new TrafficLimiterBaseTest(requestIntervalTrafficLimiter).test(true);
    }

}
