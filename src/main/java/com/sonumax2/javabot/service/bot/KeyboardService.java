package com.sonumax2.javabot.service.bot;

import com.sonumax2.javabot.commands.helper.CbParts;
import com.sonumax2.javabot.model.reference.BaseRefEntity;
import com.sonumax2.javabot.commands.helper.Cb;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeyboardService {
    private final LocalizationService localizationService;

    public KeyboardService(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    public ReplyKeyboard mainMenu(Long chatId) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(localizationService.getLocaleMessage(chatId, "menu.about"));
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(localizationService.getLocaleMessage(chatId, "menu.language"));
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(localizationService.getLocaleMessage(chatId, "menu.help"));
        keyboard.add(row3);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

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

    public InlineKeyboardMarkup operationsAddMenuInline(Long chatId, String prefix, String backCallback) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "advance", Cb.makeCb(prefix, CbParts.ADV)),
                btn(chatId, "syp", Cb.makeCb(prefix, CbParts.SYP))
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "exp", Cb.makeCb(prefix, CbParts.EXP)),
                btn(chatId, "fuel", Cb.makeCb(prefix, CbParts.FUEL))
        );
        InlineKeyboardRow row3 = row(
                btn(chatId, "back", backCallback)
        );

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, row3))
                .build();
    }

    public InlineKeyboardMarkup datePickerInline(Long chatId, String prefix, String backCallback) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "date.today", Cb.makeCb(prefix, CbParts.DATE, CbParts.TODAY)),
                btn(chatId, "date.yesterday", Cb.makeCb(prefix, CbParts.DATE, CbParts.YESTERDAY))
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "date.manual", Cb.makeCb(prefix, CbParts.DATE, CbParts.MANUAL))
        );
        InlineKeyboardRow row3 = row(
                btn(chatId, "back", backCallback)
        );
        return InlineKeyboardMarkup.builder().keyboard(List.of(row1, row2, row3)).build();
    }

    public <T extends BaseRefEntity> InlineKeyboardMarkup listTwoInOneInline(long chatId,
                                                                             List<T> list,
                                                                             String pickPrefix,
                                                                             String addCallback,
                                                                             String backCallback) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (int i = 0; i < list.size(); i += 2) {
            InlineKeyboardRow r = new InlineKeyboardRow();

            T a = list.get(i);
            r.add(btnText(a.getName(), Cb.makeCb(pickPrefix, a.getId())));

            if (i + 1 < list.size()) {
                T b = list.get(i + 1);
                r.add(btnText(b.getName(), Cb.makeCb(pickPrefix, b.getId())));
            }

            rows.add(r);
        }

        rows.add(row(btn(chatId, "add", addCallback)));
        rows.add(row(btn(chatId, "back", backCallback)));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public <T extends BaseRefEntity> InlineKeyboardMarkup listOneInline(long chatId,
                                                                             List<T> list,
                                                                             String pickPrefix,
                                                                             String addCallback,
                                                                             String backCallback) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (T item : list) {
            rows.add(row(btnText(item.getName(), Cb.makeCb(pickPrefix, item.getId()))));
        }

        rows.add(row(btn(chatId, "add", addCallback)));
        rows.add(row(btn(chatId, "back", backCallback)));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup languageInline(Long chatId) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "language.ru", Cb.makeCb(CbParts.LANG, "ru")),
                btn(chatId, "language.en", Cb.makeCb(CbParts.LANG, "en"))
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "back", CbParts.MENU)
        );
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .build();
    }

    public InlineKeyboardMarkup confirmInline(Long chatId, String prefix) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "btnSave", Cb.makeCb(prefix, CbParts.CONFIRM, CbParts.SAVE)),
                btn(chatId, "btnCancel", Cb.makeCb(prefix, CbParts.CONFIRM, CbParts.CANCEL))
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "btnEditDate", Cb.makeCb(prefix, CbParts.CONFIRM, CbParts.EDIT_DATE)),
                btn(chatId, "btnEditAmount", Cb.makeCb(prefix, CbParts.CONFIRM, CbParts.EDIT_AMOUNT))
        );
        InlineKeyboardRow row3 = row(
                btn(chatId, "btnEditNote", Cb.makeCb(prefix, CbParts.CONFIRM, CbParts.EDIT_NOTE))
        );
        return InlineKeyboardMarkup.builder().keyboard(List.of(row1, row2, row3)).build();
    }

    public InlineKeyboardMarkup skipInline(Long chatId, String skipCallback, String backCallback) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "skip", skipCallback)
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "back", backCallback)
        );
        return InlineKeyboardMarkup.builder().keyboard(List.of(row1, row2)).build();
    }

    public InlineKeyboardMarkup backInline(Long chatId, String backCallback) {
        InlineKeyboardRow row = row(
                btn(chatId, "back", backCallback)
        );

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();
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

    private InlineKeyboardButton btnText(String text, String data) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(data)
                .build();
    }

}
