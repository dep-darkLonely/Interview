package com.spring.transaction.demo.config;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Configuration
public class DataSourceConfig {
	
	
	@Bean
	@Primary
	@ConfigurationProperties(prefix = "spring.datasource")
	public PooledDataSource pooledDataSource() {
		return new PooledDataSource();
	}
	
}
