package com.sonumax2.javabot.bot.commands.universal;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.bot.ui.PanelMode;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.sonumax2.javabot.util.TelegramCommandUtils.extractCommand;

@Order(10)
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
        return extractCommand(update).filter("help"::equals).isPresent();
    }

    @Override
    public void handle(Update update) {
        if (update.hasCallbackQuery()) {
            var cq = update.getCallbackQuery();
            long chatId = cq.getMessage().getChatId();
            int messageId = cq.getMessage().getMessageId();

            ui.ack(cq.getId());
            ui.setPanelId(chatId, messageId);
            showHelpPanel(chatId, PanelMode.EDIT);
            return;
        }

        long chatId = update.getMessage().getChatId();
        showHelpPanel(chatId, PanelMode.MOVE_DOWN);
    }

    private void showHelpPanel(long chatId, PanelMode mode) {
        ui.panelKey(
                chatId,
                mode,
                "help.text",
                keyboardService.backInline(chatId, CbParts.MENU)
        );
    }

    @Override
    public String getCommand() {
        return CommandName.HELP.getName();
    }
}
