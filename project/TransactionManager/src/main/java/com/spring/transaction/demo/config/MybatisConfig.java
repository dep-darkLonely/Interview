package com.spring.transaction.demo.config;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisConfig {
	
	@Value("${mybatis.configuration.cache-enabled}")
	private boolean cacheEnable;
	
	@Value("${mybatis.configuration.default-statement-timeout}")
	private int statementTimeOut;
	
	@Value("${mybatis.configuration.default-fetch-size}")
	private int fetchSize;
	
	
	@Bean
	ConfigurationCustomizer configurationCustomizer() {
		return new ConfigurationCustomizer() {
			@Override
			public void customize(org.apache.ibatis.session.Configuration configuration) {
				configuration.setDatabaseId("transaction");
				// 启用缓存
				configuration.setCacheEnabled(cacheEnable);
				// 执行sql的响应时间
				configuration.setDefaultStatementTimeout(statementTimeOut);
				// 默认fetch 的大小
				configuration.setDefaultFetchSize(fetchSize);
			}
		};
	}
}
