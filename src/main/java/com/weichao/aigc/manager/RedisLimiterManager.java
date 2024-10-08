package com.weichao.aigc.manager;


import com.weichao.aigc.common.ErrorCode;
import com.weichao.aigc.exception.ThrowUtils;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 提供 RedisLimiter 限流基础服务
 */
@Component
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;


    /**
     * 限流操作
     * @param key 区分不同的限流器，比如不同用户 id 应该分别统计
     */
    public void doRateLimiter(String key) {
        // 创建一个名称为user_limiter的限流器，每秒最多访问 2 次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 限流器的统计规则(每秒2个请求;连续的请求,最多只能有1个请求被允许通过)
        // RateType.OVERALL表示速率限制作用于整个令牌桶,即限制所有请求的速率
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
        // 每当一个操作来了后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        // 如果没有令牌,还想执行操作,就抛出异常
        ThrowUtils.throwIf(!canOp, ErrorCode.TOO_MANY_REQUEST);
    }

}
