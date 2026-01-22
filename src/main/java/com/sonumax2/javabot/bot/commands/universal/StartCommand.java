package com.sonumax2.javabot.bot.commands.universal;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.domain.session.UserState;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.sonumax2.javabot.util.TelegramCommandUtils.extractCommand;

@Component
public class StartCommand implements Command {

    private final KeyboardService keyboardService;
    private final UserSessionService userSessionService;
    private final BotUi ui;

    public StartCommand(
            KeyboardService keyboardService,
            UserSessionService userSessionService,
            BotUi ui) {
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

            ui.ack(cq.getId());

            showMainMenuEditMessage(chatId, messageId);
            return;
        }

        showMainMenuNewMessage(update.getMessage().getChatId());
    }

    private void showMainMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.IDLE);
        String name = userSessionService.displayName(chatId);
        ui.editKey(
                chatId,
                messageId,
                "menu.welcome",
                keyboardService.mainMenuInline(chatId),
                name
        );
    }

    private void showMainMenuNewMessage(Long chatId) {
        userSessionService.setUserState(chatId, UserState.IDLE);
        String name = userSessionService.displayName(chatId);
        ui.sendKey(chatId,
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
