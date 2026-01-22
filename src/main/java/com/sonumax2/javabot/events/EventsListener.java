package com.sonumax2.javabot.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.concurrent.Executor;

@Component
public class EventsListener {

    private static final Logger log = LoggerFactory.getLogger(EventsListener.class);

    private final TelegramClient telegramClient;
    private final Executor telegramExecutor;

    public EventsListener(TelegramClient telegramClient,
                          @Qualifier("telegramExecutor") Executor telegramExecutor) {
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
            String details = describe(method);
            log.error("Telegram execute failed: {} {}", method.getClass().getSimpleName(), details, ex);

            if (event.getFuture() != null) {
                event.getFuture().completeExceptionally(ex);
            }
        }
    }

    private String describe(BotApiMethod<? extends Serializable> method) {
        try {
            if (method instanceof SendMessage m) {
                return "(chatId=" + m.getChatId()
                        + ", textLen=" + safeLen(m.getText())
                        + ", hasMarkup=" + (m.getReplyMarkup() != null) + ")";
            }
            if (method instanceof EditMessageText m) {
                return "(chatId=" + m.getChatId()
                        + ", messageId=" + m.getMessageId()
                        + ", textLen=" + safeLen(m.getText())
                        + ", hasMarkup=" + (m.getReplyMarkup() != null) + ")";
            }
            if (method instanceof EditMessageReplyMarkup m) {
                return "(chatId=" + m.getChatId()
                        + ", messageId=" + m.getMessageId()
                        + ", hasMarkup=" + (m.getReplyMarkup() != null) + ")";
            }
            if (method instanceof DeleteMessage m) {
                return "(chatId=" + m.getChatId()
                        + ", messageId=" + m.getMessageId() + ")";
            }
            if (method instanceof AnswerCallbackQuery m) {
                return "(callbackQueryId=" + m.getCallbackQueryId()
                        + ", alert=" + m.getShowAlert()
                        + ", textLen=" + safeLen(m.getText()) + ")";
            }

            return "(method=" + String.valueOf(method) + ")";
        } catch (Exception ignored) {
            return "(details_failed)";
        }
    }

    private int safeLen(String s) {
        return s == null ? 0 : s.length();
    }
}
