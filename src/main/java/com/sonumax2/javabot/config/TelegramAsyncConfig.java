package com.sonumax2.javabot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class TelegramAsyncConfig {

    @Bean
    public Executor telegramExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tg-executor");
            t.setDaemon(true);
            return t;
        });
    }
}