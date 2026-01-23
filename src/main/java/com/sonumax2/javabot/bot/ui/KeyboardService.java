package com.sonumax2.javabot.bot.ui;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.domain.reference.BaseRefEntity;
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
    private static final int BTN_TEXT_MAX = 50; // держим запас под 64 байта callback_data

    private final LocalizationService localizationService;

    public KeyboardService(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    // ---------------- reply keyboards ----------------

    public ReplyKeyboard mainMenu(Long chatId) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(textRow(chatId, "menu.about"));
        keyboard.add(textRow(chatId, "menu.language"));
        keyboard.add(textRow(chatId, "menu.help"));

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return keyboardMarkup;
    }

    // ---------------- inline keyboards ----------------

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

    public InlineKeyboardMarkup datePickerInline(Long chatId, String prefix, String backCallback) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "date.today", cb(prefix, CbParts.DATE, CbParts.TODAY)),
                btn(chatId, "date.yesterday", cb(prefix, CbParts.DATE, CbParts.YESTERDAY))
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "date.manual", cb(prefix, CbParts.DATE, CbParts.MANUAL))
        );

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, backRow(chatId, backCallback)))
                .build();
    }


    public <T extends BaseRefEntity> InlineKeyboardMarkup listTwoInOneInline(
            long chatId,
            List<T> list,
            String pickPrefix,
            String addCallback,
            String backCallback
    ) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (int i = 0; i < list.size(); i += 2) {
            T a = list.get(i);

            if (i + 1 < list.size()) {
                T b = list.get(i + 1);
                rows.add(row(
                        refBtn(a, pickPrefix),
                        refBtn(b, pickPrefix)
                ));
            } else {
                rows.add(row(refBtn(a, pickPrefix)));
            }
        }

        rows.add(row(btn(chatId, "add", addCallback)));
        rows.add(backRow(chatId, backCallback));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public <T extends BaseRefEntity> InlineKeyboardMarkup listTwoInOneInlineWithSkip(
            long chatId,
            List<T> list,
            String pickPrefix,
            String addCallback,
            String skipCallback,
            String backCallback
    ) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (int i = 0; i < list.size(); i += 2) {
            T a = list.get(i);

            if (i + 1 < list.size()) {
                T b = list.get(i + 1);
                rows.add(row(
                        refBtn(a, pickPrefix),
                        refBtn(b, pickPrefix)
                ));
            } else {
                rows.add(row(refBtn(a, pickPrefix)));
            }
        }

        rows.add(row(btn(chatId, "add", addCallback)));
        rows.add(row(btn(chatId, "skip", skipCallback)));
        rows.add(backRow(chatId, backCallback));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }


    public <T extends BaseRefEntity> InlineKeyboardMarkup listOneInline(
            long chatId,
            List<T> list,
            String pickPrefix,
            String addCallback,
            String backCallback
    ) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (T item : list) {
            rows.add(row(refBtn(item, pickPrefix)));
        }

        rows.add(row(btn(chatId, "add", addCallback)));
        rows.add(backRow(chatId, backCallback));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

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

    public InlineKeyboardMarkup confirmInline(Long chatId, String prefix) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "btnSave", cb(prefix, CbParts.CONFIRM, CbParts.SAVE)),
                btn(chatId, "btnCancel", cb(prefix, CbParts.CONFIRM, CbParts.CANCEL))
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "btnEditDate", cb(prefix, CbParts.CONFIRM, CbParts.EDIT_DATE)),
                btn(chatId, "btnEditAmount", cb(prefix, CbParts.CONFIRM, CbParts.EDIT_AMOUNT))
        );
        InlineKeyboardRow row3 = row(
                btn(chatId, "btnEditNote", cb(prefix, CbParts.CONFIRM, CbParts.EDIT_NOTE))
        );

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, row3))
                .build();
    }

    public InlineKeyboardMarkup skipInline(Long chatId, String skipCallback, String backCallback) {
        InlineKeyboardRow row1 = row(btn(chatId, "skip", skipCallback));
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, backRow(chatId, backCallback)))
                .build();
    }

    public InlineKeyboardMarkup backInline(Long chatId, String backCallback) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(backRow(chatId, backCallback)))
                .build();
    }

    public InlineKeyboardMarkup receiptInline(Long chatId, String pickPrefix, String backCallback) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "receipt.has", Cb.makeCb(pickPrefix, "RECEIPT")),
                btn(chatId, "receipt.invoice", Cb.makeCb(pickPrefix, "INVOICE"))
        );
        InlineKeyboardRow row2 = row(
                btn(chatId, "receipt.none", Cb.makeCb(pickPrefix, "NO_RECEIPT"))
        );

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, backRow(chatId, backCallback)))
                .build();
    }

    public InlineKeyboardMarkup confirmExpenseInline(Long chatId, String prefix, boolean showAttach, String attachBtnKey) {
        InlineKeyboardRow row1 = row(
                btn(chatId, "btnSave", cb(prefix, CbParts.CONFIRM, CbParts.SAVE)),
                btn(chatId, "btnCancel", cb(prefix, CbParts.CONFIRM, CbParts.CANCEL))
        );

        InlineKeyboardRow row2 = row(
                btn(chatId, "btnEditObject", cb(prefix, CbParts.CONFIRM, CbParts.EDIT_OBJECT)),
                btn(chatId, "btnEditItem",   cb(prefix, CbParts.CONFIRM, CbParts.EDIT_ITEM))
        );

        InlineKeyboardRow row3 = row(
                btn(chatId, "btnEditCp",  cb(prefix, CbParts.CONFIRM, CbParts.EDIT_CP)),
                btn(chatId, "btnEditDoc", cb(prefix, CbParts.CONFIRM, CbParts.EDIT_DOC))
        );

        InlineKeyboardRow row4 = row(
                btn(chatId, "btnEditDate",   cb(prefix, CbParts.CONFIRM, CbParts.EDIT_DATE)),
                btn(chatId, "btnEditAmount", cb(prefix, CbParts.CONFIRM, CbParts.EDIT_AMOUNT))
        );

        InlineKeyboardRow row5 = row();
        if (showAttach) {
            row5 = row(
                    btn(chatId, attachBtnKey, cb(prefix, CbParts.CONFIRM, CbParts.ATTACH_FILE)),
                    btn(chatId, "btnEditNote", cb(prefix, CbParts.CONFIRM, CbParts.EDIT_NOTE))
            );
        } else {
            row5 = row(
                    btn(chatId, "btnEditNote", cb(prefix, CbParts.CONFIRM, CbParts.EDIT_NOTE))
            );
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, row3, row4, row5))
                .build();
    }

    public InlineKeyboardMarkup backSkipInline(long chatId, String backCallback, String skipCallback) {
        InlineKeyboardRow row = row(
                btn(chatId, "btnBack", backCallback),
                btn(chatId, "btnSkip", skipCallback)
        );
        return InlineKeyboardMarkup.builder().keyboard(List.of(row)).build();
    }


    // ---------------- small builders ----------------

    private InlineKeyboardRow backRow(Long chatId, String backCallback) {
        return row(btn(chatId, "back", backCallback));
    }

    private KeyboardRow textRow(Long chatId, String textKey) {
        KeyboardRow row = new KeyboardRow();
        row.add(localizationService.getLocaleMessage(chatId, textKey));
        return row;
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

    private <T extends BaseRefEntity> InlineKeyboardButton refBtn(T ref, String pickPrefix) {
        return btnText(cutBtnText(ref.getName()), cb(pickPrefix, ref.getId()));
    }

    private String cb(Object... parts) {
        return Cb.makeCb(parts);
    }

    private String cutBtnText(String s) {
        if (s == null) return "";
        return s.length() <= BTN_TEXT_MAX ? s : s.substring(0, BTN_TEXT_MAX - 1) + "…";
    }
}
