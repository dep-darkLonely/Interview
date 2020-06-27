package com.redis.demo.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;

/**
 * Redis 配置 哨兵模式
 */
@Configurable
public class RedisSentinelModeConfiguration {

    @Value("${spring.redis.sentinel.master}")
    private String master;

    @Value("${spring.redis.sentinel.node}")
    private String sentinel;

    @Value("${spring.redis.sentinel.password}")
    private String password;

    @Autowired
    private JedisPoolConfigurationProperties jedisPoolConfigurationProperties;
//
//    @Bean
//    public JedisConnectionFactory jedisConnectionFactory() {
//        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration()
//                .master(master);
//
//        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
//        jedisPoolConfig.setMaxIdle(jedisPoolConfigurationProperties.getMaxIdle());
//        jedisPoolConfig.setMaxWaitMillis(jedisPoolConfigurationProperties.getMaxWait());
//        jedisPoolConfig.setMinIdle(jedisPoolConfigurationProperties.getMinIdle());
//        jedisPoolConfig.setMaxTotal(jedisPoolConfigurationProperties.getMaxActive());
//
//        return new JedisConnectionFactory(redisSentinelConfiguration, jedisPoolConfig);
//    }
}
