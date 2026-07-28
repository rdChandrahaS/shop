package com.shop.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisConfig {
	
	@Bean
	StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		return new StringRedisTemplate(connectionFactory);
	}
	
	@Bean
	DefaultRedisScript<Boolean> paymentLockScript(){
		DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
		
		script.setLocation(new ClassPathResource("paymentLockScript.lua"));
		script.setResultType(Boolean.class);
		
		return script;
	}
}
