package com.sonumax2.javabot.bot.commands.ops;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.domain.session.UserSession;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class AddOperationCommand implements Command {

    private final KeyboardService keyboardService;
    private final BotUi ui;
    private final UserSessionService userSessionService;

    public AddOperationCommand(
            KeyboardService keyboardService,
            BotUi ui, UserSessionService userSessionService
    ) {
        this.ui = ui;
        this.keyboardService = keyboardService;
        this.userSessionService = userSessionService;
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
        int clickedMessageId = cq.getMessage().getMessageId();

        userSessionService.setPanelMessageId(chatId, clickedMessageId);

        showAddOperationPanel(chatId);
    }

    private void showAddOperationPanel(long chatId) {
        Long panelId = userSessionService.getPanelMessageId(chatId);

        var kb = keyboardService.operationsAddMenuInline(chatId, CbParts.ADD_OPR, CbParts.MENU);

        if (panelId == null) {
            ui.movePanelDownKey(chatId, null, "operation.text", kb);
            return;
        }

        ui.editKey(chatId, panelId.intValue(), "operation.text", kb);
    }

    @Override
    public String getCommand() {
       return CommandName.ADD_OPERATION.getName();
    }
}
