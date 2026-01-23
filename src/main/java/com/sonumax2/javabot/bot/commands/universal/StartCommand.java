package com.sonumax2.javabot.bot.commands.universal;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.domain.session.UserState;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.sonumax2.javabot.util.TelegramCommandUtils.extractCommand;

@Order(10)
@Component
public class StartCommand implements Command {

    private final KeyboardService keyboardService;
    private final BotUi ui;
    private final UserSessionService userSessionService;

    public StartCommand(
            KeyboardService keyboardService,
            BotUi ui, UserSessionService userSessionService) {
        this.ui = ui;
        this.keyboardService = keyboardService;
        this.userSessionService = userSessionService;
    }

    @Override
    public boolean canHandle(Update update) {
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            String data = update.getCallbackQuery().getData();
            return Cb.is(data, CbParts.START) || Cb.is(data, CbParts.MENU);
        }
        return extractCommand(update).filter(CbParts.START::equals).isPresent();
    }

    @Override
    public void handle(Update update) {
        if (update.hasCallbackQuery()) {
            var cq = update.getCallbackQuery();
            long chatId = cq.getMessage().getChatId();
            int messageId = cq.getMessage().getMessageId();

            userSessionService.setUserState(chatId, UserState.IDLE);

            ui.ack(cq.getId());
            ui.setPanelId(chatId, messageId);
            showStartPanel(chatId, PanelMode.EDIT);
            return;
        }

        long chatId = update.getMessage().getChatId();
        userSessionService.setUserState(chatId, UserState.IDLE);
        showStartPanel(chatId, PanelMode.MOVE_DOWN);
    }

    private void showStartPanel(long chatId, PanelMode mode) {
        String name = userSessionService.displayName(chatId);
        ui.panelKey(
                chatId,
                mode,
                "menu.welcome",
                keyboardService.mainMenuInline(chatId),
                name
        );
    }

    @Override
    public String getCommand() {
        return CommandName.START.getName();
    }
}
