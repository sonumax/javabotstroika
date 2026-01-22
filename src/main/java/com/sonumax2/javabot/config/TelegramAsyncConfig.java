package com.sonumax2.javabot.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Configuration
public class TelegramAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(TelegramAsyncConfig.class);

    private ExecutorService telegramExecutor;

    @Bean(name = "telegramExecutor")
    public ExecutorService telegramExecutor() {
        if (telegramExecutor != null) return telegramExecutor;

        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "telegram-exec");
            t.setUncaughtExceptionHandler((th, ex) ->
                    log.error("Uncaught exception in {}", th.getName(), ex)
            );
            t.setDaemon(false);
            return t;
        };

        telegramExecutor = Executors.newSingleThreadExecutor(tf);
        return telegramExecutor;
    }

    @PreDestroy
    public void shutdown() {
        if (telegramExecutor == null) return;

        telegramExecutor.shutdown();
        try {
            if (!telegramExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("telegramExecutor did not terminate in time, forcing shutdownNow()");
                telegramExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            telegramExecutor.shutdownNow();
        }
    }
}
