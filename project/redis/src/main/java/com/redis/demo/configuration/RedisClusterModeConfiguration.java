package com.redis.demo.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 集群模式配置
 */
@Configuration
public class RedisClusterModeConfiguration {

    @Autowired
    ClusterConfigurationProperties clusterConfigurationProperties;

    @Autowired
    private JedisPoolConfigurationProperties jedisPoolConfigurationProperties;

//    @Bean
//    public JedisConnectionFactory connectionFactory() {
//        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
//        jedisPoolConfig.setMaxIdle(jedisPoolConfigurationProperties.getMaxIdle());
//        jedisPoolConfig.setMaxWaitMillis(jedisPoolConfigurationProperties.getMaxWait());
//        jedisPoolConfig.setMinIdle(jedisPoolConfigurationProperties.getMinIdle());
//        jedisPoolConfig.setMaxTotal(jedisPoolConfigurationProperties.getMaxActive());
//
//        return new JedisConnectionFactory(
//                new RedisClusterConfiguration(clusterConfigurationProperties.getNodes()),
//                jedisPoolConfig
//        );
//    }
}
