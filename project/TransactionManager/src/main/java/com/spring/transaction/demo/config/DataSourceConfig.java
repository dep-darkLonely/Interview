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
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getDriverClassName() {
		return driverClassName;
	}
	
	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPingQuery() {
		return pingQuery;
	}
	
	public void setPingQuery(String pingQuery) {
		this.pingQuery = pingQuery;
	}
	
	public int getMaximumActiveConnections() {
		return maximumActiveConnections;
	}
	
	public void setMaximumActiveConnections(int maximumActiveConnections) {
		this.maximumActiveConnections = maximumActiveConnections;
	}
	
	public int getMaximumIdleConnections() {
		return maximumIdleConnections;
	}
	
	public void setMaximumIdleConnections(int maximumIdleConnections) {
		this.maximumIdleConnections = maximumIdleConnections;
	}
	
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
