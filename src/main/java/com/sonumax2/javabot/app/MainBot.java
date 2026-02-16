package com.sonumax2.javabot.app;

import com.sonumax2.javabot.bot.commands.CommandHandler;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.domain.auth.AuthService;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class MainBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final CommandHandler commandHandler;
    private final String botToken;
    private final UserSessionService userSessionService;
    private final AuthService authService;
    private final BotUi botUi;

    private static final Logger log = LoggerFactory.getLogger(MainBot.class);

    public MainBot(
            @Value("${bot.token}") String botToken,
            CommandHandler commandHandler,
            UserSessionService userSessionService,
            AuthService authService,
            BotUi botUi) {
        this.botToken = botToken;
        this.commandHandler = commandHandler;
        this.userSessionService = userSessionService;
        this.authService = authService;
        this.botUi = botUi;
    }

    @Override public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        try {
            Long chatId = extractChatId(update);
            if (chatId != null) {
                String firstName = extractFirstName(update);
                String username = extractUsername(update);

                var auth = authService.checkOrBootstrap(chatId, firstName, username);
                if (!auth.allowed()) {
                    if (auth.replyText() != null) botUi.sendText(chatId, auth.replyText());
                    return;
                }
            }

            userSessionService.touchFromUpdate(update);
            commandHandler.handle(update);

        } catch (Throwable e) {
            log.error("Unhandled error while processing update: {}", update, e);
        }
    }

    private Long extractChatId(Update update) {
        if (update.hasMessage() && update.getMessage() != null) {
            return update.getMessage().getChatId();
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }

    private String extractFirstName(Update update) {
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            return update.getMessage().getFrom().getFirstName();
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            return update.getCallbackQuery().getFrom().getFirstName();
        }
        return null;
    }

    private String extractUsername(Update update) {
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            return update.getMessage().getFrom().getUserName();
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            return update.getCallbackQuery().getFrom().getUserName();
        }
        return null;
    }

}