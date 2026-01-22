package com.sonumax2.javabot.bot.ui;

import com.sonumax2.javabot.events.MessageEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class BotMessageService {
    private static final long EXEC_TIMEOUT_SEC = 10;

    private final ApplicationEventPublisher publisher;

    public BotMessageService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /** Асинхронно отправить (fire-and-forget). Результат не нужен. */
    public void send(BotApiMethod<?> method) {
        if (method == null) return;
        // future = null => EventsListener просто выполнит и всё
        publisher.publishEvent(new MessageEvent(this, cast(method), null));
    }

    /** Отправить и дождаться результата (нужно редко: когда важен messageId/Message). */
    public <T extends Serializable> T execute(BotApiMethod<T> method) {
        if (method == null) throw new IllegalArgumentException("Telegram method is null");

        CompletableFuture<Serializable> future = new CompletableFuture<>();
        publisher.publishEvent(new MessageEvent(this, cast(method), future));

        try {
            @SuppressWarnings("unchecked")
            T res = (T) future.get(EXEC_TIMEOUT_SEC, TimeUnit.SECONDS);
            return res;
        } catch (TimeoutException e) {
            throw new RuntimeException("Telegram execute timeout: " + method.getClass().getSimpleName(), e);
        } catch (Exception e) {
            throw new RuntimeException("Telegram execute failed: " + method.getClass().getSimpleName(), e);
        }
    }

    // -------- common actions --------

    public void safeDelete(long chatId, int messageId) {
        try {
            delete(chatId, messageId);
        } catch (Exception ignored) {
        }
    }

    public void delete(long chatId, int messageId) {
        send(DeleteMessage.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .build());
    }

    public void answerCallback(String callbackQueryId) {
        if (callbackQueryId == null) return;
        send(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .build());
    }

    public void editText(long chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        EditMessageText.EditMessageTextBuilder b = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(text == null ? "" : text);

        if (markup != null) b.replyMarkup(markup);
        send(b.build());
    }

    public void editMarkup(long chatId, int messageId, InlineKeyboardMarkup markup) {
        send(EditMessageReplyMarkup.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .replyMarkup(markup)
                .build());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BotApiMethod<? extends Serializable> cast(BotApiMethod<?> m) {
        return (BotApiMethod) m;
    }
}
