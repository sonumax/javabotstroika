package com.sonumax2.javabot.service.bot;

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

@Service
public class BotMessageService {
    private final ApplicationEventPublisher publisher;

    public BotMessageService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void send(BotApiMethod<? extends Serializable> method) {
        publisher.publishEvent(new MessageEvent(this, method, null));
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public <T extends Serializable> T execute(BotApiMethod<T> method) {
        CompletableFuture<Serializable> future = new CompletableFuture<>();
        publisher.publishEvent(new MessageEvent(this, (BotApiMethod) method, future));
        return (T) future.join();
    }

    public void delete(long chatId, int messageId) {
        send(DeleteMessage.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .build());
    }

    public void answerCallback(String callbackQueryId) {
        send(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .build());
    }

    public void editText(long chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        var b = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(text);

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
}
