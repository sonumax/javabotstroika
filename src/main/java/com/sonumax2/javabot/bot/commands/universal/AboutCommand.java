package com.sonumax2.javabot.bot.commands.universal;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.sonumax2.javabot.util.TelegramCommandUtils.extractCommand;

@Component
public class AboutCommand implements Command {

    private final KeyboardService keyboardService;
    private final BotUi ui;

    public AboutCommand(
            KeyboardService keyboardService,
            BotUi ui) {
        this.keyboardService = keyboardService;
        this.ui = ui;
    }

    @Override
    public boolean canHandle(Update update) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            return Cb.is(data, CbParts.ABOUT);
        }
        return extractCommand(update).filter("/about"::equals).isPresent();
    }

    @Override
    public void handle(Update update) {
        if (update.hasCallbackQuery()) {
            var cq = update.getCallbackQuery();
            long chatId = cq.getMessage().getChatId();
            int messageId = cq.getMessage().getMessageId();

            ui.ack(cq.getId());
            showAboutMenuEditMessage(chatId, messageId);
            return;
        }

        long chatId = update.getMessage().getChatId();
        showAboutMenuNewMessage(chatId);
    }

    private void showAboutMenuEditMessage(long chatId, int messageId) {
        ui.editKey(
                chatId,
                messageId,
                "about.text",
                keyboardService.backInline(chatId, CbParts.MENU)
        );
    }

    private void showAboutMenuNewMessage(long chatId) {
        ui.sendKey(
                chatId,
                "about.text",
                keyboardService.backInline(chatId, CbParts.MENU)
        );
    }

    @Override
    public String getCommand() {
        return CommandName.ABOUT.getName();
    }
}
