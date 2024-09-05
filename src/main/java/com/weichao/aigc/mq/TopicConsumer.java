package com.weichao.aigc.mq;

import com.rabbitmq.client.*;

public class TopicConsumer {

    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        String queueName = "frontend_queue";
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "#.frontend.#");

        String queueName2 = "backend_queue";
        channel.queueDeclare(queueName2, false, false, false, null);
        channel.queueBind(queueName2, EXCHANGE_NAME, "#.backend.#");

        String queueName3 = "product_queue";
        channel.queueDeclare(queueName3, false, false, false, null);
        channel.queueBind(queueName3, EXCHANGE_NAME, "#.product.#");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback frontendDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [frontend] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        DeliverCallback backendDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [backend] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        DeliverCallback productDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [product] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        channel.basicConsume(queueName, true, frontendDeliverCallback, consumerTag -> { });
        channel.basicConsume(queueName2, true, backendDeliverCallback, consumerTag -> { });
        channel.basicConsume(queueName3, true, productDeliverCallback, consumerTag -> { });
    }
}
