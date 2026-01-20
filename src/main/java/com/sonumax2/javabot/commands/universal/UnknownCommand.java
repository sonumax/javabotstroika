package com.sonumax2.javabot.commands.universal;

import com.sonumax2.javabot.commands.helper.Command;
import com.sonumax2.javabot.service.bot.BotUi;
import com.sonumax2.javabot.service.bot.KeyboardService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class UnknownCommand implements Command {

    private final KeyboardService keyboardService;
    private final BotUi ui;

    public UnknownCommand(KeyboardService keyboardService, BotUi ui) {
        this.keyboardService = keyboardService;
        this.ui = ui;
    }

    @Override
    public boolean canHandle(Update update) {
        return update.hasMessage() && update.getMessage().hasText();
    }

    @Override
    public void handle(Update update) {
        Long chatId = update.getMessage().getChatId();
        ui.msg(
                chatId,
                "unknown.command",
                keyboardService.mainMenuInline(chatId)
        );
    }

    @Override
    public String getCommand() {
        return "unknown";
    }
}
