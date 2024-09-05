package com.weichao.aigc.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TtlConsumer {

    /**
     * 正在监听的队列名
     */
    private final static String QUEUE_NAME = "ttl_queue";

    public static void main(String[] argv) throws Exception {
        // 创建连接，创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        // 从工厂获取一个新的连接
        Connection connection = factory.newConnection();
        // 从连接中创建一个新的频道
        Channel channel = connection.createChannel();

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-message-ttl", 5000);
        channel.queueDeclare(QUEUE_NAME, false, false, false, args);

        // 控制台打印等待的接受的消息
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // 定义如何处理消息，创建一个新的 DeliverCallback 来处理接收到的消息
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            // 将消息体转换为已接收的消息
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            // 在控制台打印已接收到的消息
            System.out.println(" [x] Received '" + message + "'");
        };
        // 在频道上开始消费队列中的消息，接收到的消息会传递给 deliverCallback 来处理，会持续阻塞
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    }
}