package com.weichao.aigc.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class RedisLimiterManagerTest {

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Test
    void doRateLimiter() throws InterruptedException {
        // 模拟一下操作
        String userId = "1";
        // 瞬时执行2次
        for (int i = 0; i < 2; i++) {
            redisLimiterManager.doRateLimiter(userId);
            System.out.println("success");

        }
        // 暂停 1 秒
        Thread.sleep(1000);
        // 瞬时执行2次
        for (int i = 0; i < 5; i++) {
            redisLimiterManager.doRateLimiter(userId);
            System.out.println("success");
        }
    }

}