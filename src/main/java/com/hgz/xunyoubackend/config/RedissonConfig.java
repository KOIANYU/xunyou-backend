package com.hgz.xunyoubackend.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类
 *
 * @author: hgz
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private String host;

    private String port;

    @Bean
    public RedissonClient redissonClient() {
        // 创建Redisson配置
        Config config = new Config();
        String redissonAddress = "redis://192.168.189.132:6379";
        config.useSingleServer()
                .setAddress(redissonAddress)
                .setPassword("123456")
                .setDatabase(1);

        // 创建Redisson实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }

}
