package com.sonumax2.javabot.bot.commands.universal;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.sonumax2.javabot.util.TelegramCommandUtils.extractCommand;

@Order(10)
@Component
public class LanguageCommand implements Command {

    private final KeyboardService keyboardService;
    private final UserSessionService userSessionService;
    private final BotUi ui;

    public LanguageCommand(
            KeyboardService keyboardService,
            UserSessionService userSessionService,
            BotUi ui
    ) {
        this.keyboardService = keyboardService;
        this.userSessionService = userSessionService;
        this.ui = ui;
    }

    @Override
    public boolean canHandle(Update update) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            return Cb.is(data, CbParts.LANG) || Cb.startsWith(data, CbParts.LANG);
        }
        return extractCommand(update).filter(cmd -> cmd.startsWith("lang")).isPresent();
    }

    @Override
    public void handle(Update update) {

        if (update.hasCallbackQuery()) {
            var cq = update.getCallbackQuery();
            long chatId = cq.getMessage().getChatId();
            int messageId = cq.getMessage().getMessageId();
            String data = cq.getData();

            ui.ack(cq.getId());
            ui.setPanelId(chatId, messageId);

            if (Cb.is(data, CbParts.LANG)) {
                showLanguagePanel(chatId, PanelMode.EDIT);
                return;
            }

            // lang:ru / lang:en
            if (Cb.startsWith(data, CbParts.LANG)) {
                String lang = CbParts.LANG + ":";
                lang = data.substring(lang.length());
                userSessionService.setLocale(chatId, lang);

                showLanguageSwitchedPanel(chatId, PanelMode.EDIT);
            }
            return;
        }
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();
        String[] parts = text.split("\\s+");

        if (parts.length >= 2) {
            String lang = parts[1];
            userSessionService.setLocale(chatId, lang);

            showLanguageSwitchedPanel(chatId, PanelMode.MOVE_DOWN);
        } else {
            showLanguagePanel(chatId, PanelMode.MOVE_DOWN);
        }
    }

    private void showLanguagePanel(long chatId, PanelMode mode) {
        ui.panelKey(
                chatId,
                mode,
                "language.select",
                keyboardService.languageInline(chatId)
        );
    }

    private void showLanguageSwitchedPanel(long chatId, PanelMode mode) {
        ui.panelKey(
                chatId,
                mode,
                "language.switched",
                keyboardService.mainMenuInline(chatId)
        );
    }

    @Override
    public String getCommand() {
        return CommandName.LANGUAGE.getName();
    }
}

