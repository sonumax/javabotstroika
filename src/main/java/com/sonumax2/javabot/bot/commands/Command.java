package com.sonumax2.javabot.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface Command {
    boolean canHandle(Update update);

    void handle(Update update);

    String getCommand();
}
