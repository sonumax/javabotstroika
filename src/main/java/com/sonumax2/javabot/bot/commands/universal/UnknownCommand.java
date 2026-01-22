package com.sonumax2.javabot.bot.commands.universal;

import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.domain.draft.ExpenseDraft;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class UnknownCommand implements Command {

    private final KeyboardService keyboardService;
    private final BotUi ui;
    private final UserSessionService userSessionService;

    public UnknownCommand(KeyboardService keyboardService, BotUi ui, UserSessionService userSessionService) {
        this.keyboardService = keyboardService;
        this.ui = ui;
        this.userSessionService = userSessionService;
    }

    @Override
    public boolean canHandle(Update update) {
        return update.hasMessage() && update.getMessage().hasText();
    }

    @Override
    public void handle(Update update) {
        Long chatId = update.getMessage().getChatId();

        Long panelId = userSessionService.getPanelMessageId(chatId);
        if (panelId != null) {
            ui.editKey(chatId, panelId.intValue(), "unknown.command", keyboardService.mainMenuInline(chatId));
        } else {
            ui.sendKey(chatId, "unknown.command", keyboardService.mainMenuInline(chatId));
        }
    }
    @Override
    public String getCommand() {
        return CommandName.UNKNOWN.getName();
    }
}
