package com.sonumax2.javabot.commands.universal;

import com.sonumax2.javabot.commands.helper.Cb;
import com.sonumax2.javabot.commands.helper.CbParts;
import com.sonumax2.javabot.commands.helper.Command;
import com.sonumax2.javabot.commands.helper.CommandName;
import com.sonumax2.javabot.service.bot.BotUi;
import com.sonumax2.javabot.service.bot.KeyboardService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.sonumax2.javabot.util.TelegramCommandUtils.extractCommand;

@Component
public class HelpCommand implements Command {

    private final KeyboardService keyboardService;
    private final BotUi ui;

    public HelpCommand(
            KeyboardService keyboardService,
            BotUi ui
    ) {
        this.keyboardService = keyboardService;
        this.ui = ui;
    }

    @Override
    public boolean canHandle(Update update) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            return Cb.is(data, CbParts.HELP);
        }
        return extractCommand(update).filter("/help"::equals).isPresent();
    }

    @Override
    public void handle(Update update) {
        if (update.hasCallbackQuery()) {
            var cq = update.getCallbackQuery();
            long chatId = cq.getMessage().getChatId();
            int messageId = cq.getMessage().getMessageId();

            ui.ack(cq.getId());
            showHelpMenuEditMessage(chatId, messageId);
            return;
        }

        long chatId = update.getMessage().getChatId();
        showHelpMenuNewMessage(chatId);
    }

    private void showHelpMenuEditMessage(long chatId, int messageId) {
        ui.editKey(
                chatId,
                messageId,
                "help.text",
                keyboardService.backInline(chatId, CbParts.MENU)
        );
    }

    private void showHelpMenuNewMessage(long chatId) {
        ui.sendKey(
                chatId,
                "help.text",
                keyboardService.backInline(chatId, CbParts.MENU)
        );
    }

    @Override
    public String getCommand() {
        return CommandName.HELP.getName();
    }
}
