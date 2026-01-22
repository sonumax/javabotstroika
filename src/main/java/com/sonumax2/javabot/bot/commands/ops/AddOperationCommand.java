package com.sonumax2.javabot.bot.commands.ops;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class AddOperationCommand implements Command {

    private final KeyboardService keyboardService;
    private final BotUi ui;

    public AddOperationCommand(
            KeyboardService keyboardService,
            BotUi ui
    ) {
        this.ui = ui;
        this.keyboardService = keyboardService;
    }

    @Override
    public boolean canHandle(Update update) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            return Cb.is(data, CbParts.ADD_OPR);
        }
        return false;
    }

    @Override
    public void handle(Update update) {
        var cq = update.getCallbackQuery();
        ui.ack(cq.getId());

        long chatId = cq.getMessage().getChatId();
        int messageId = cq.getMessage().getMessageId();

        showAddOperationEditMessage(chatId, messageId);
    }

    private void showAddOperationEditMessage(long chatId, int messageId) {
        ui.editKey(chatId,
                messageId,
                "operation.text",
                keyboardService.operationsAddMenuInline(chatId, CbParts.ADD_OPR, CbParts.MENU)
        );
    }

    @Override
    public String getCommand() {
       return CommandName.ADD_OPERATION.getName();
    }
}
