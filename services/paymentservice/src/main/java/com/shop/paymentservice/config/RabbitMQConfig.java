package com.shop.paymentservice.config;


import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@Configuration
public class RabbitMQConfig {
	
	@Value("${payment.exchange.key}")
    private String EXCHANGE;

    @Value("${payment.request.queue}")
    private String PAYMENT_REQUEST_QUEUE;

    @Value("${rabbitmq.dlx.name}")
    private String DLX_NAME;

    @Value("${rabbitmq.dlq.routing.key}")
    private String DLQ_ROUTING_KEY;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }
	
    @Bean
    public Queue paymentRequestQueue() {
        return QueueBuilder.durable(PAYMENT_REQUEST_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }
    
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}
