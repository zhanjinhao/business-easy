package cn.addenda.businesseasy.trafficlimit;

/**
 * 令牌桶限流：限制请求的时间间隔 & 在闲时存一批令牌 & 没有桶存等待中的请求
 *
 * @author addenda
 * @datetime 2022/12/29 18:42
 */
public class TokenBucketTrafficLimiter implements TrafficLimiter {

    /**
     * 令牌桶的容量
     */
    private final long capacity;
    /**
     * 每秒产生的令牌的数量
     */
    private final double permitsPerSecond;
    /**
     * 上一次颁发token的时间
     */
    private long lastTokenTime;
    /**
     * 当前令牌数量
     */
    private long tokens;

    public TokenBucketTrafficLimiter(long capacity, double permitsPerSecond) {
        this.capacity = capacity;
        this.permitsPerSecond = permitsPerSecond;
    }

    @Override
    public synchronized long tryAcquire() {
        long now = System.currentTimeMillis();

        // 必须使用取整，不能使用Math.round()
        long newPermits = (long) ((now - lastTokenTime) * permitsPerSecond / 1000);
        tokens = Math.min(tokens + newPermits, capacity);

        if (tokens > 0) {
            lastTokenTime = now;
            tokens--;
            return 0;
        }
        return -1;
    }

    @Override
    public boolean acquire() {
        return tryAcquire() == 0;
    }

}
