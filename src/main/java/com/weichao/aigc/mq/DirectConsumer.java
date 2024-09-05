package com.weichao.aigc.mq;

import com.rabbitmq.client.*;

public class DirectConsumer {

    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);

        String queueName = "info_warning_error_queue";
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "all_log");

        String queueName2 = "warning_error_queue";
        channel.queueDeclare(queueName2, false, false, false, null);
        channel.queueBind(queueName2, EXCHANGE_NAME, "error_log");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback allLogDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [log] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        DeliverCallback errorLogDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [error] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        channel.basicConsume(queueName, true, allLogDeliverCallback, consumerTag -> {});
        channel.basicConsume(queueName2, true, errorLogDeliverCallback, consumerTag -> {});
    }
}
