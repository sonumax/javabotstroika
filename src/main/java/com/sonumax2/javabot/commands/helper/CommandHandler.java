package com.sonumax2.javabot.commands.helper;

import com.sonumax2.javabot.commands.universal.UnknownCommand;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Collection;

@Service
public class CommandHandler {
    private final Collection<Command> commands;

    public CommandHandler(Collection<Command> commands) {
        this.commands = commands;
    }

    public void handle(Update update) {
        Command fallback = null;

        for (Command command : commands) {
            if (!command.canHandle(update)) continue;

            // fallback-команду не выполняем сразу
            if (command instanceof UnknownCommand) {
                fallback = command;
                continue;
            }

            // нашли нормальную команду — выполняем и выходим
            command.handle(update);
            return;
        }

        // если ничего не подошло — выполняем fallback
        if (fallback != null) {
            fallback.handle(update);
        }
    }
}
