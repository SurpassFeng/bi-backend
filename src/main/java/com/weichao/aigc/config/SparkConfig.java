package com.weichao.aigc.config;


import io.github.briqt.spark4j.SparkClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 星火大模型 Websocket 服务接口配置
 */
@Configuration
@ConfigurationProperties(prefix = "spark.client")
@Data
public class SparkConfig {

    /**
     * APPID
     */
    private String appid;

    /**
     * APISecret
     */
    private String apiSecret;


    /**
     * APIKey
     */
    private String apiKey;


    /**
     * Websocket服务接口认证信息
     *
     * @return
     */
    @Bean
    public SparkClient sparkClient(){
        SparkClient sparkClient = new SparkClient();
        sparkClient.appid = appid;
        sparkClient.apiSecret = apiSecret;
        sparkClient.apiKey = apiKey;
        return sparkClient;
    }

}
