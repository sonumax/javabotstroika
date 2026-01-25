package com.sonumax2.javabot.bot.ui;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.domain.reference.BaseRefEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public final class Kb {

    private Kb() {}

    public static InlineKeyboardRow row(InlineKeyboardButton... buttons) {
        return new InlineKeyboardRow(buttons);
    }

    public static InlineKeyboardButton btn(LocalizationService ls, Long chatId, String textKey, String data) {
        return InlineKeyboardButton.builder()
                .text(ls.getLocaleMessage(chatId, textKey))
                .callbackData(data)
                .build();
    }

    public static InlineKeyboardButton btnText(String text, String data) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(data)
                .build();
    }

    public static KeyboardRow textRow(LocalizationService ls, Long chatId, String textKey) {
        KeyboardRow row = new KeyboardRow();
        row.add(ls.getLocaleMessage(chatId, textKey));
        return row;
    }

    public static InlineKeyboardRow backRow(LocalizationService ls, Long chatId, String backCallback) {
        return row(btn(ls, chatId, "back", backCallback));
    }

    public static <T extends BaseRefEntity> InlineKeyboardButton refBtn(T ref, String pickPrefix, int maxText) {
        return btnText(cut(ref.getName(), maxText), Cb.makeCb(pickPrefix, ref.getId()));
    }

    public static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
