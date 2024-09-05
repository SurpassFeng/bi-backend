package com.weichao.aigc.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MyMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息到交换机
     * @param exchange 指定消息到哪个交换机
     * @param routingKey 路由键，指定消息去哪个队列
     * @param msg 消息
     */
    public void sendMessage(String exchange, String routingKey, String msg) {
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
    }

}
