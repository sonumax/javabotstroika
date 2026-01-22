package com.sonumax2.javabot.bot.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Service
public class CommandHandler {
    private final List<Command> commands;
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    public CommandHandler(List<Command> commands) {
        this.commands = commands;
    }

    public void handle(Update update) {
        for (Command command : commands) {
            if (!command.canHandle(update)) continue;
            command.handle(update);
            return;
        }
        log.debug("No command matched update: {}", update);
    }
}
