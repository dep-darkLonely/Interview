package com.redis.demo.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;

/**
 * Redis 配置 主从复制模式
 */
@Configurable
public class RedisMasterReplicaConfiguration {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.database}")
    private int index;

    @Autowired
    private JedisPoolConfigurationProperties jedisPoolConfigurationProperties;

    /**
     * 获取JedisConnectionFactory
     * @return JedisConnectionFactory 实例
     */
//    @Bean
//    public JedisConnectionFactory redisConnectionFactory() {
//
//
//        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
//        configuration.setHostName(host);
//        configuration.setPort(port);
//        configuration.setPassword(password);
//        configuration.setDatabase(index);
//
//        return new JedisConnectionFactory(configuration);
//    }
}
