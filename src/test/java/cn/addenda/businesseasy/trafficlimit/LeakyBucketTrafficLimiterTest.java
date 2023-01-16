package cn.addenda.businesseasy.trafficlimit;

import org.junit.Test;

/**
 * @author addenda
 * @datetime 2022/12/28 15:14
 */
public class LeakyBucketTrafficLimiterTest {

    @Test
    public void test1() throws Exception {
        LeakyBucketTrafficLimiter leakyBucketTrafficLimiter = new LeakyBucketTrafficLimiter(200L, 1);
        new TrafficLimiterBaseTest(leakyBucketTrafficLimiter).test(true);
    }

}
