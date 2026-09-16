package com.shop.orderingservice.config;

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

    @Value("${payment.request.queue}")
    private String paymentRequestQueueName;

    @Value("${payment.response.queue}")
    private String paymentResponseQueueName;

    @Value("${payment.exchange.key}")
    private String exchangeName;

    @Value("${payment.request.routing.key}")
    private String paymentRequestRoutingKey;

    @Value("${payment.result.routing.key}")
    private String paymentResultRoutingKey;

    @Value("${payment.request.dlx.name}")
    private String paymentRequestDlxName;

    @Value("${payment.request.dlq.name}")
    private String paymentRequestDlqName;

    @Value("${payment.request.dlq.routing.key}")
    private String paymentRequestDlqRoutingKey;

    @Value("${ordering.dlx.name}")
    private String orderingDlxName;

    @Value("${ordering.dlq.name}")
    private String orderingDlqName;

    @Value("${ordering.dlq.routing.key}")
    private String orderingDlqRoutingKey;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public TopicExchange foodExchange() {
        return new TopicExchange("food.exchange");
    }

    // Payment request queue is owned by the payment flow, so its DLQ config
    // must match the declaration in paymentservice exactly.
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

    // Ordering-owned queues share one ordering DLQ because the same service
    // handles their failures.
    @Bean
    public DirectExchange orderingDeadLetterExchange() {
        return new DirectExchange(orderingDlxName);
    }

    @Bean
    public Queue orderingDeadLetterQueue() {
        return QueueBuilder.durable(orderingDlqName).build();
    }

    @Bean
    public Binding orderingDlqBinding() {
        return BindingBuilder.bind(orderingDeadLetterQueue())
                .to(orderingDeadLetterExchange())
                .with(orderingDlqRoutingKey);
    }

    @Bean
    public Queue paymentResponseQueue() {
        return QueueBuilder.durable(paymentResponseQueueName)
                .withArgument("x-dead-letter-exchange", orderingDlxName)
                .withArgument("x-dead-letter-routing-key", orderingDlqRoutingKey)
                .build();
    }

    @Bean
    public Binding requestBinding() {
        return BindingBuilder.bind(paymentRequestQueue())
                .to(exchange())
                .with(paymentRequestRoutingKey);
    }

    @Bean
    public Binding responseBinding() {
        return BindingBuilder.bind(paymentResponseQueue())
                .to(exchange())
                .with(paymentResultRoutingKey);
    }

    @Bean
    public Queue foodUpdateQueue() {
        return QueueBuilder.durable("ordering_food_update_queue")
                .withArgument("x-dead-letter-exchange", orderingDlxName)
                .withArgument("x-dead-letter-routing-key", orderingDlqRoutingKey)
                .build();
    }

    @Bean
    public Binding foodUpdateBinding() {
        return BindingBuilder.bind(foodUpdateQueue())
                .to(foodExchange())
                .with("food.update");
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}
