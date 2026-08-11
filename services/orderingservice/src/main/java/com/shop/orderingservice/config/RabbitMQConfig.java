package com.shop.orderingservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
	
	@Value("${payment.request.queue}")
	public String PAYMENT_REQUEST_QUEUE;
	
	@Value("${payment.response.queue}")
    public String PAYMENT_RESPONSE_QUEUE;
	
	@Value("${payment.exchange.key}")
    public String EXCHANGE;
	
	@Value("${payment.request.routing.key}")
    public String PAYMENT_REQUEST_ROUTING_KEY;
	
	@Value("${payment.result.routing.key}")
    public String PAYMENT_RESULT_ROUTING_KEY;
    
    
	@Bean
	public Queue paymentRequestQueue() {
		return new Queue(PAYMENT_REQUEST_QUEUE, true);
	}
	
	@Bean
	public Queue paymentResponseQueue() {
		return new Queue(PAYMENT_RESPONSE_QUEUE, true);
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
							.with(PAYMENT_RESPONSE_QUEUE);
	}
	
	@Bean
	public MessageConverter converter() {
		return new JacksonJsonMessageConverter();
	}
}
