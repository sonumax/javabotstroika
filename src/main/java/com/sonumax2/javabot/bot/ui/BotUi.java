package com.sonumax2.javabot.bot.ui;

import com.sonumax2.javabot.domain.session.service.UserSessionService;
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
    private final UserSessionService userSessionService;

    public BotUi(BotMessageService bot, LocalizationService localeMessage, UserSessionService userSessionService) {
        this.bot = bot;
        this.localeMessage = localeMessage;
        this.userSessionService = userSessionService;
    }

    // -------- localization --------

    public String msg(long chatId, String key, Object... args) {
        return localeMessage.getLocaleMessage(chatId, key, args);
    }

    // -------- panel rendering (NEW) --------
    // Цель: один метод решает — редактировать текущую панель или создать новую “снизу”

    public int panelKey(long chatId, PanelMode mode, String key, InlineKeyboardMarkup kb, Object... args) {
        return panelText(chatId, mode, msg(chatId, key, args), kb);
    }

    public int panelText(long chatId, PanelMode mode, String text, InlineKeyboardMarkup kb) {
        Integer current = panelId(chatId);

        // панели нет — просто создаём как панель
        if (current == null) {
            return sendPanelTextReturnId(chatId, text, kb);
        }

        if (mode == PanelMode.MOVE_DOWN) {
            return replacePanelText(chatId, current, text, kb);
        }

        // EDIT (fallback если edit не удался)
        boolean ok = bot.tryEditTextSync(chatId, current, text, kb);
        if (ok) return current;

        // если edit упал — создаём новое сообщение, удаляем старое, и panelMessageId обновится в sendPanelTextReturnId()
        return replacePanelText(chatId, current, text, kb);
    }

    public Integer panelId(long chatId) {
        return toInt(userSessionService.getPanelMessageId(chatId));
    }

    public void setPanelId(long chatId, int messageId) {
        userSessionService.setPanelMessageId(chatId, messageId);
    }

    private Integer toInt(Long v) {
        return v == null ? null : v.intValue();
    }

    // -------- send (async) --------

    public void sendText(long chatId, String text) {
        sendText(chatId, text, null);
    }

    public void sendText(long chatId, String text, ReplyKeyboard kb) {
        bot.send(sm(chatId, text, kb));
    }

    public void sendKey(long chatId, String key, ReplyKeyboard kb, Object... args) {
        sendText(chatId, msg(chatId, key, args), kb);
    }

    /**
     * Создать панель (как новое сообщение) и записать её id в session.
     * (Без удаления старой панели — иногда это нужно.)
     */
    public int sendPanelKey(long chatId, String key, ReplyKeyboard kb, Object... args) {
        return sendPanelTextReturnId(chatId, msg(chatId, key, args), kb);
    }

    // -------- edit (async) --------

    public void editKey(long chatId, int messageId, String key, InlineKeyboardMarkup kb, Object... args) {
        bot.editText(chatId, messageId, msg(chatId, key, args), kb);
    }

    public void editText(long chatId, int messageId, String text, InlineKeyboardMarkup kb) {
        bot.editText(chatId, messageId, text, kb);
    }

    // -------- callback (async) --------

    public void ack(String callbackQueryId) {
        if (callbackQueryId == null) return;
        bot.answerCallback(callbackQueryId);
    }

    public void toast(String callbackQueryId, String text, boolean alert) {
        if (callbackQueryId == null) return;
        bot.send(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .text(text)
                .showAlert(alert)
                .build());
    }

    public void toastKey(long chatId, String callbackQueryId, String key, boolean alert, Object... args) {
        toast(callbackQueryId, msg(chatId, key, args), alert);
    }

    // -------- delete (async) --------

    public void delete(long chatId, int messageId) {
        bot.safeDelete(chatId, messageId);
    }

    // -------- need result (blocking execute) --------

    /** Нужен messageId — поэтому execute() */
    public int sendKeyReturnId(long chatId, String key, ReplyKeyboard kb, Object... args) {
        Message m = bot.execute(sm(chatId, msg(chatId, key, args), kb));
        return m.getMessageId();
    }

    /** Создать новую панель (ниже), удалить старую */
    public int movePanelDownKey(long chatId, Integer oldPanelId, String key, ReplyKeyboard kb, Object... args) {
        return replacePanelText(chatId, oldPanelId, msg(chatId, key, args), kb);
    }

    // -------- replace (blocking execute) --------

    /** Отправить новое сообщение-панель, удалить старое, и обновить panelMessageId в session */
    public int replacePanelText(long chatId, Integer oldMessageId, String text, ReplyKeyboard kb) {
        int newId = sendPanelTextReturnId(chatId, text, kb);

        if (oldMessageId != null && oldMessageId != newId) {
            bot.safeDelete(chatId, oldMessageId);
        }
        return newId;
    }

    public int replacePanelKey(long chatId, Integer oldMessageId, String key, ReplyKeyboard kb, Object... args) {
        return replacePanelText(chatId, oldMessageId, msg(chatId, key, args), kb);
    }

    public void notify(long chatId, String key, Object... args) {
        sendKey(chatId, key, null, args);
    }

    // -------- internal --------

    private int sendPanelTextReturnId(long chatId, String text, ReplyKeyboard kb) {
        Message m = bot.execute(sm(chatId, text, kb));
        int id = m.getMessageId();
        userSessionService.setPanelMessageId(chatId, id);
        return id;
    }

    private SendMessage sm(long chatId, String text, ReplyKeyboard kb) {
        var b = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text == null ? "" : text);

        if (kb != null) b.replyMarkup(kb);
        return b.build();
    }
}
