package com.sonumax2.javabot.bot.ui;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

@Service
public class KeyboardService {

    private final LocalizationService localizationService;

    public KeyboardService(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    // ---------------- main menu ----------------

    public InlineKeyboardMarkup mainMenuInline(Long chatId) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "menu.opr", CbParts.ADD_OPR)
        );

        InlineKeyboardRow row2 = row(
                btn(chatId, "menu.help", CbParts.HELP),
                btn(chatId, "menu.about", CbParts.ABOUT),
                btn(chatId, "menu.language", CbParts.LANG)
        );

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .build();
    }

    // ---------------- operations ----------------

    /** Меню выбора операции (кнопки стартуют flow через callbacks типа addOpr:adv) */
    public InlineKeyboardMarkup operationsAddMenuInline(Long chatId, String prefix, String backCallback) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "advance", cb(prefix, CbParts.ADV)),
                btn(chatId, "syp", cb(prefix, CbParts.SYP))
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "exp", cb(prefix, CbParts.EXP)),
                btn(chatId, "fuel", cb(prefix, CbParts.FUEL))
        );

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, backRow(chatId, backCallback)))
                .build();
    }

    // ---------------- language ----------------

    public InlineKeyboardMarkup languageInline(Long chatId) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "language.ru", cb(CbParts.LANG, "ru")),
                btn(chatId, "language.en", cb(CbParts.LANG, "en"))
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "back", CbParts.MENU)
        );

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .build();
    }

    // ---------------- generic ----------------

    public InlineKeyboardMarkup backInline(Long chatId, String backCallback) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(backRow(chatId, backCallback)))
                .build();
    }

    // ---------------- small builders ----------------

    private InlineKeyboardRow backRow(Long chatId, String backCallback) {
        return row(btn(chatId, "back", backCallback));
    }

    private InlineKeyboardRow row(InlineKeyboardButton... buttons) {
        return new InlineKeyboardRow(buttons);
    }

    private InlineKeyboardButton btn(Long chatId, String textKey, String data) {
        return InlineKeyboardButton.builder()
                .text(localizationService.getLocaleMessage(chatId, textKey))
                .callbackData(data)
                .build();
    }

    private String cb(Object... parts) {
        return Cb.makeCb(parts);
    }
}
