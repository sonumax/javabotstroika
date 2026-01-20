package com.sonumax2.javabot.events;

import org.springframework.context.ApplicationEvent;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class MessageEvent extends ApplicationEvent {

    private final BotApiMethod<? extends Serializable> method;
    private final CompletableFuture<Serializable> future; // может быть null

    public MessageEvent(Object source,
                        BotApiMethod<? extends Serializable> method,
                        CompletableFuture<Serializable> future) {
        super(source);
        this.method = Objects.requireNonNull(method, "method");
        this.future = future;
    }

    public BotApiMethod<? extends Serializable> getMethod() {
        return method;
    }

    public CompletableFuture<Serializable> getFuture() {
        return future;
    }
}