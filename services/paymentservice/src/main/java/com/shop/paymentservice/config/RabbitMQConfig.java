package com.shop.paymentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${payment.exchange.key}")
    private String exchangeName;

    @Value("${payment.request.queue}")
    private String paymentRequestQueueName;

    @Value("${payment.request.dlx.name}")
    private String paymentRequestDlxName;

    @Value("${payment.request.dlq.name}")
    private String paymentRequestDlqName;

    @Value("${payment.request.dlq.routing.key}")
    private String paymentRequestDlqRoutingKey;

    @Value("${payment.request.routing.key}")
    private String paymentRequestRoutingKey;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public DirectExchange paymentRequestDeadLetterExchange() {
        return new DirectExchange(paymentRequestDlxName);
    }

    @Bean
    public Queue paymentRequestDeadLetterQueue() {
        return QueueBuilder.durable(paymentRequestDlqName).build();
    }

    @Bean
    public Binding paymentRequestDlqBinding() {
        return BindingBuilder.bind(paymentRequestDeadLetterQueue())
                .to(paymentRequestDeadLetterExchange())
                .with(paymentRequestDlqRoutingKey);
    }

    @Bean
    public Queue paymentRequestQueue() {
        return QueueBuilder.durable(paymentRequestQueueName)
                .withArgument("x-dead-letter-exchange", paymentRequestDlxName)
                .withArgument("x-dead-letter-routing-key", paymentRequestDlqRoutingKey)
                .build();
    }

    @Bean
    public Binding requestBinding() {
        return BindingBuilder.bind(paymentRequestQueue())
                .to(exchange())
                .with(paymentRequestRoutingKey);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}
