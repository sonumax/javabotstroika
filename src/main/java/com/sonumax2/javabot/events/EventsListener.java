package com.sonumax2.javabot.events;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.concurrent.Executor;

@Component
public class EventsListener {

    private static final Logger log = LoggerFactory.getLogger(EventsListener.class);

    private final TelegramClient telegramClient;
    private final Executor telegramExecutor;

    public EventsListener(TelegramClient telegramClient, @Qualifier("telegramExecutor") Executor telegramExecutor) {
        this.telegramClient = telegramClient;
        this.telegramExecutor = telegramExecutor;
    }

    @EventListener
    public void on(MessageEvent event) {
        telegramExecutor.execute(() -> executeSafely(event));
    }

    private void executeSafely(MessageEvent event) {
        BotApiMethod<? extends Serializable> method = event.getMethod();
        try {
            @SuppressWarnings({"rawtypes","unchecked"})
            Serializable res = telegramClient.execute((BotApiMethod) method);

            if (event.getFuture() != null) {
                event.getFuture().complete(res);
            }
        } catch (Exception ex) {
            log.error("Telegram execute failed: {}", method.getClass().getSimpleName(), ex);

            if (event.getFuture() != null) {
                event.getFuture().completeExceptionally(ex);
            }
        }
    }
}

