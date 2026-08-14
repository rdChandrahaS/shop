package com.shop.messageservice.config;

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
	
	@Value("${rabbitmq.queue.name}")
	private String queueName;
	
	@Value("${rabbitmq.exchange.name}")
	private String exchangeName;
	
	@Value("${rabbitmq.routing.key}")
	private String routingKey;

	@Value("${rabbitmq.dlx.name}")
	private String dlxName;
	
	@Value("${rabbitmq.dlq.name}")
	private String dlqName;
	
	@Value("${rabbitmq.dlq.routing.key}")
	private String dlqRoutingKey;

	// --- 1. Dead Letter Queue & Exchange ---
	@Bean
	public DirectExchange deadLetterExchange() {
		return new DirectExchange(dlxName);
	}
	
	@Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }
		
	@Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(dlqRoutingKey);
    }
	
	// --- 2. Primary Queue (Linked to DLX) ---
	@Bean
	public Queue queue() {
		return QueueBuilder.durable(queueName)
				.withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
				.build();
	}
	
	// --- 3. Primary Exchange & Binding ---
	@Bean
	public TopicExchange exchange() {
		return new TopicExchange(exchangeName);
	}
	
	@Bean
	public Binding binding() {
		return BindingBuilder.bind(queue())
							 .to(exchange())
							 .with(routingKey);
	}
	
	@Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}