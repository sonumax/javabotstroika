package com.sonumax2.javabot.commands.universal;

import com.sonumax2.javabot.commands.helper.Cb;
import com.sonumax2.javabot.commands.helper.CbParts;
import com.sonumax2.javabot.commands.helper.Command;
import com.sonumax2.javabot.commands.helper.CommandName;
import com.sonumax2.javabot.service.bot.BotUi;
import com.sonumax2.javabot.service.bot.KeyboardService;
import com.sonumax2.javabot.service.bot.UserSessionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.sonumax2.javabot.util.TelegramCommandUtils.extractCommand;

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

            if (Cb.is(data, CbParts.LANG)) {
                showLanguageMenuEditMessage(chatId, messageId);
                return;
            }

            // lang:ru / lang:en
            if (Cb.startsWith(data, CbParts.LANG)) {
                String lang = CbParts.LANG + ":";
                lang = data.substring(lang.length());
                userSessionService.setLocale(chatId, lang);

                showLanguageSwitchedMenuEditMessage(chatId, messageId);
            }
            return;
        }

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();
        String[] parts = text.split("\\s+");

        if (parts.length >= 2) {
            String lang = parts[1];
            userSessionService.setLocale(chatId, lang);
            showLanguageSwitchedMenuNewMessage(chatId);
        } else {
            showLanguageMenuNewMessage(chatId);
        }
    }

    private void showLanguageMenuNewMessage(long chatId) {
        ui.sendKey(
                chatId,
                "language.select",
                keyboardService.languageInline(chatId)
        );
    }

    private void showLanguageSwitchedMenuNewMessage(long chatId) {
        ui.sendKey(
                chatId,
                "language.switched",
                keyboardService.mainMenuInline(chatId)
        );
    }

    private void showLanguageSwitchedMenuEditMessage(long chatId, int messageId) {
        ui.editKey(
                chatId,
                messageId,
                "language.switched",
                keyboardService.mainMenuInline(chatId)
        );
    }

    private void showLanguageMenuEditMessage(long chatId, int messageId) {
        ui.editKey(
                chatId,
                messageId,
                "language.select",
                keyboardService.languageInline(chatId)
        );
    }

    @Override
    public String getCommand() {
        return CommandName.LANGUAGE.getName();
    }
}

