package com.sonumax2.javabot;

import com.sonumax2.javabot.commands.helper.CommandHandler;
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

    public MainBot(@Value("${bot.token}") String botToken, CommandHandler commandHandler) {
        this.botToken = botToken;
        this.commandHandler = commandHandler;
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
        commandHandler.handle(update);
    }
}