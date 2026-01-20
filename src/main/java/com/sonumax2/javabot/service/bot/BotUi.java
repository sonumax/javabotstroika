package com.sonumax2.javabot.service.bot;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

@Service
public class BotUi {

    private final BotMessageService bot;
    private final LocalizationService localeMessage;

    public BotUi(BotMessageService bot, LocalizationService localeMessage) {
        this.bot = bot;
        this.localeMessage = localeMessage;
    }

    public String msg(long chatId, String key, Object... args) {
        return localeMessage.getLocaleMessage(chatId, key, args);
    }

    /* -------- send -------- */

    public void sendText(long chatId, String text) {
        sendText(chatId, text, null);
    }

    public void sendText(long chatId, String text, ReplyKeyboard kb) {
        bot.send(SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(kb)
                .build());
    }

    public void sendKey(long chatId, String key, ReplyKeyboard kb, Object... args) {
        sendText(chatId, msg(chatId, key, args), kb);
    }

    /* -------- edit -------- */
    public void editKey(long chatId, int messageId, String key, InlineKeyboardMarkup kb, Object... args) {
        bot.editText(chatId, messageId, msg(chatId, key, args), kb);
    }

    public void editText(long chatId, int messageId, String text, InlineKeyboardMarkup kb) {
        bot.editText(chatId, messageId, text, kb);
    }

    /* -------- callback -------- */
    public void ack(String callbackQueryId) {
        bot.answerCallback(callbackQueryId);
    }

    public void toast(String callbackQueryId, String text, boolean alert) {
        bot.send(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .text(text)
                .showAlert(alert)
                .build());
    }

    public void toastKey(long chatId, String callbackQueryId, String key, boolean alert, Object... args) {
        toast(callbackQueryId, msg(chatId, key, args), alert);
    }

    /* -------- delete -------- */
    public void delete(long chatId, int messageId) {
        bot.delete(chatId, messageId);
    }

    public int sendKeyReturnId(long chatId, String key, ReplyKeyboard kb, Object... args) {
        SendMessage sm = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(msg(chatId, key, args))
                .replyMarkup(kb)
                .build();

        Message m = bot.execute(sm);
        return m.getMessageId();
    }

    public int movePanelDownKey(long chatId, Integer oldPanelId, String key, ReplyKeyboard kb, Object... args) {
        int newPanelId = sendKeyReturnId(chatId, key, kb, args);

        if (oldPanelId != null && !oldPanelId.equals(newPanelId)) {
            delete(chatId, oldPanelId);
        }
        return newPanelId;
    }

    /* -------- replace -------- */
    public int replacePanelText(long chatId, Integer oldMessageId, String text, ReplyKeyboard kb) {
        Message m = bot.execute(SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(kb)
                .build());

        int newId = m.getMessageId();

        if (oldMessageId != null && oldMessageId != newId) {
            bot.delete(chatId, oldMessageId);
        }
        return newId;
    }

    public int replacePanelKey(long chatId, Integer oldMessageId, String key, ReplyKeyboard kb, Object... args) {
        return replacePanelText(chatId, oldMessageId, msg(chatId, key, args), kb);
    }
}
