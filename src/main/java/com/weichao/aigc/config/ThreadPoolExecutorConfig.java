package com.weichao.aigc.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "spring.thread-pool")
@Data
public class ThreadPoolExecutorConfig {

    private int corePoolSize;

    private int maxPoolSize;

    private int queueCapacity;

    private int keepAliveSeconds;

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            // 初始化线程数为 1
            private int count = 1;

            // 每当线程池需要创建新线程时，就会调用newThread方法
            // @NotNull Runnable r 表示方法参数 r 应该永远不为null，
            // 如果这个方法被调用的时候传递了一个null参数，就会报错
            @Override
            public Thread newThread(@NotNull Runnable r) {
                // 创建一个新的线程
                Thread thread = new Thread(r);
                // 给新线程设置一个名称，名称中包含线程数的当前值
                thread.setName("线程" + count);
                // 线程数递增
                count++;
                // 返回新创建的线程
                return thread;
            }


        };
        // 创建一个新的线程池，线程池核心大小为2，最大线程数为4，
        // 非核心线程空闲时间为100秒，任务队列为阻塞队列，长度为4，使用自定义的线程工厂创建线程
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), threadFactory);
        // 返回创建的线程池
        return threadPoolExecutor;
    }
}




