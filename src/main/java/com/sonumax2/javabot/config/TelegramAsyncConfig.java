package com.sonumax2.javabot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class TelegramAsyncConfig {

    @Bean(name = "telegramExecutor", destroyMethod = "shutdown")
    public ExecutorService telegramExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tg-executor");
            t.setDaemon(true);
            return t;
        });
    }
}
