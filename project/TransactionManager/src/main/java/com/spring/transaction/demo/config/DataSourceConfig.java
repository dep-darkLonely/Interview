package com.spring.transaction.demo.config;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceConfig {
	
	// url
	private String url;
	// 数据库驱动
	private String driverClassName;
	// 用户名
	private String username;
	
	private String password;
	
	// test
	private String pingQuery;
	
	// 最大激活连接数
	private int maximumActiveConnections;
	
	// 最大空闲连接数
	private int maximumIdleConnections;
	
	@Bean
	@Primary
	public PooledDataSource pooledDataSource(
	) {
		PooledDataSource pooledDataSource = new PooledDataSource();
		pooledDataSource.setDriver(driverClassName);
		pooledDataSource.setUrl(url);
		pooledDataSource.setUsername(username);
		pooledDataSource.setPassword(password);
		pooledDataSource.setPoolPingQuery(pingQuery);
		pooledDataSource.setPoolMaximumActiveConnections(maximumActiveConnections);
		pooledDataSource.setPoolMaximumIdleConnections(maximumIdleConnections);
		return pooledDataSource;
	}
	
}
