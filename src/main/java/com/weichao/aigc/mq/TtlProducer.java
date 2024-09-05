package com.weichao.aigc.mq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

/**
 * 消息发送
 *
 * @author weichao
 */
public class TtlProducer {

    /**
     * 消息队列的名字
     */
    private final static String QUEUE_NAME = "ttl_queue";

    public static void main(String[] argv) throws Exception {
        // 用于连接 RabbitMQ 服务器
        ConnectionFactory factory = new ConnectionFactory();
        // 连接本机
        factory.setHost("localhost");

        // 使用ConnectionFactory创建一个新的连接,这个连接用于和RabbitMQ服务器进行交互
        try (Connection connection = factory.newConnection();
             // 通过已建立的连接创建一个新的频道
             Channel channel = connection.createChannel()) {

            // 在通道上声明一个队列，我们在此指定的队列名为"hello"
//            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            // 创建要发送的消息，这里我们将要发送的消息内容设置为"Hello World!"
            String message = "Hello World!";

            // 给消息指定过期时间
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    // 设置消息的过期时间为1000毫秒
                    .expiration("1000")
                    .build();
            // 发布消息到指定的交换机（"my-exchange"）和路由键（"routing-key"）
            // 使用指定的属性（过期时间）和消息内容（UTF-8编码的字节数组）
            channel.basicPublish("my-exchange", "routing-key", properties, message.getBytes(StandardCharsets.UTF_8));

            // 使用channel.basicPublish方法将消息发布到指定的队列中。这里我们指定的队列名为"hello"
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
}


