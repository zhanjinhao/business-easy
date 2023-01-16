package cn.addenda.businesseasy.trafficlimit;

/**
 * @author addenda
 * @datetime 2022/12/28 14:43
 */
public class RequestLogTrafficLimiterTest {

    public static void main(String[] args) throws Exception {
        RequestLogTrafficLimiter requestLogTrafficLimiter = new RequestLogTrafficLimiter(10, 1000);
        new TrafficLimiterBaseTest(requestLogTrafficLimiter).test(true);
    }

}
