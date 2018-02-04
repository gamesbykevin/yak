package com.coinbase.exchange.api.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

/**
 * Created by robevansuk on 07/02/2017.
 */
@SpringBootConfiguration
@Configuration
@ComponentScan(basePackages="com.gamesbykevin.tradingbot.agent")
public class GdaxConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(5);
        pool.setMaxPoolSize(10);
        pool.setWaitForTasksToCompleteOnShutdown(true);
        return pool;
    }
}