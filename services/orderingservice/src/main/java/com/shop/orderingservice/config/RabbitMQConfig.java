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
	private String PAYMENT_REQUEST_QUEUE;
	
	@Value("${payment.response.queue}")
	private String PAYMENT_RESPONSE_QUEUE;
	
	@Value("${payment.exchange.key}")
	private String EXCHANGE;
	
	@Value("${payment.request.routing.key}")
	private String PAYMENT_REQUEST_ROUTING_KEY;
	
	@Value("${payment.result.routing.key}")
	private String PAYMENT_RESULT_ROUTING_KEY;
	
	@Value("${rabbitmq.dlx.name}")
	private String DLX_NAME;
	
	@Value("${rabbitmq.dlq.name}")
	private String DLQ_NAME;
	
	@Value("${rabbitmq.dlq.routing.key}")
	private String DLQ_ROUTING_KEY;
    
    
	@Bean
	public DirectExchange deadLetterExchange() {
		return new DirectExchange(DLX_NAME);
	}
	
	@Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }
		
	@Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }
	
	// --- Primary Queue Definitions (Updated to route to DLX on failure) ---
    @Bean
    public Queue paymentRequestQueue() {
        return QueueBuilder.durable(PAYMENT_REQUEST_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }
	
    @Bean
    public Queue paymentResponseQueue() {
        return QueueBuilder.durable(PAYMENT_RESPONSE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }
	
	@Bean
	public TopicExchange exchange() {
		return new TopicExchange(EXCHANGE);
	}
	
	@Bean
	public Binding requestBinding() {
		return BindingBuilder.bind(paymentRequestQueue())
							.to(exchange())
							.with(PAYMENT_REQUEST_ROUTING_KEY);
	}
	
	@Bean
	public Binding responseBinding() {
	    return BindingBuilder.bind(paymentResponseQueue())
	            .to(exchange())
	            .with(PAYMENT_RESULT_ROUTING_KEY); 
	}
	
	@Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}
