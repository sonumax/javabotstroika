package com.sonumax2.javabot.app;

import com.sonumax2.javabot.bot.commands.CommandHandler;
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

    private static final Logger log = LoggerFactory.getLogger(MainBot.class);

    public MainBot(@Value("${bot.token}") String botToken, CommandHandler commandHandler, UserSessionService userSessionService) {
        this.botToken = botToken;
        this.commandHandler = commandHandler;
        this.userSessionService = userSessionService;
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
            userSessionService.touchFromUpdate(update);
            commandHandler.handle(update);
        } catch (Throwable e) {
            log.error("Unhandled error while processing update: {}", update, e);
        }
    }
}