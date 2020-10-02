package com.redis.demo.Redisson;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:application.properties")
public class RedissonConfig {
	
	@Value("${spring.redis.address}")
	private String address;

	/**
	 * 创建redisson实例
	 * Sync and Async API
	 */
	@Bean("redisson")
	public RedissonClient redissonClient() {
		Config config = new Config();
		// 这里只设置了一个redis的地址
		config.useSingleServer()
				.setAddress(address);
		return Redisson.create(config);
	}

}
