package com.shop.messageservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
	
	//Spring Bean for RabbitMQ Queue
	@Bean
	public Queue queue() {
		return new Queue(queueName);
	}
	
	//Spring bean for RabbitMQ Exchange
	@Bean
	public TopicExchange exchange() {
		return new TopicExchange(exchangeName);
	}
	
	//Spring Bean for RabbitMQ Queue and Exchange Binding
	@Bean
	public Binding binding() {
		return BindingBuilder.bind(queue())
							 .to(exchange())
							 .with(routingKey);
	}
	
	@Bean
	public MessageConverter converter() {
		return new JacksonJsonMessageConverter();
	}
	
	@Bean
	public RabbitTemplate template(ConnectionFactory connectionFactory) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(converter());
		return rabbitTemplate;
	}
	
	//Spring boot auto configuration will auto-configure these 3 beans for us. we don't have to explicitly create these beans
	
	//Spring Bean for RabbitMQ Template
	//Spring Bean for ConnectionFactory
	//Spring Bean for RabbitAdmin
}
