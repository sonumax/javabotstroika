package com.sonumax2.javabot.bot.commands.universal;

import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.FlowEngine;
import com.sonumax2.javabot.bot.flow.FlowRegistry;
import com.sonumax2.javabot.domain.session.UserState;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Order(1)
@Service
public class FlowRouterCommand implements Command {

    private final UserSessionService session;
    private final FlowRegistry registry;
    private final FlowEngine engine;

    public FlowRouterCommand(UserSessionService session, FlowRegistry registry, FlowEngine engine) {
        this.session = session;
        this.registry = registry;
        this.engine = engine;
    }

    @Override
    public boolean canHandle(Update update) {
        if (update == null) return false;

        // 0) START COMMANDS: /advance, /expense, ...
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            if (text != null && text.startsWith("/")) {
                return registry.getByCommand(text) != null;
            }
        }

        // 1) CALLBACKS: ns:...
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            String data = update.getCallbackQuery().getData();

            String ns = extractNs(data);
            if (ns != null && registry.get(ns) != null) return true;

            return registry.getByStartCallback(data) != null;
        }

        // 2) MESSAGES: текст/фото только когда активен flow
        if (!update.hasMessage()) return false;

        long chatId = update.getMessage().getChatId();

        if (session.getUserState(chatId) != UserState.FLOW_WAIT_INPUT) return false;

        String ns = session.getActiveFlowNs(chatId);
        if (ns == null || ns.isBlank()) return false;
        if (registry.get(ns) == null) return false;

        if (update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            return text != null && !text.startsWith("/");
        }

        return update.getMessage().hasPhoto() || update.getMessage().hasDocument();
    }

    private String extractNs(String data) {
        if (data == null) return null;
        int p = data.indexOf(':');
        if (p <= 0) return null;
        return data.substring(0, p);
    }


    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handle(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            if (text != null && text.startsWith("/")) {
                FlowDefinition def = registry.getByCommand(text);
                if (def != null) {
                    engine.startFromMessage(update, def);
                }
                return;
            }
        }

        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();

            String ns = extractNs(data);
            FlowDefinition def = (ns != null) ? registry.get(ns) : null;
            if (def == null) def = registry.getByStartCallback(data);

            if (def != null) engine.handle(update, def);
            return;
        }

        long chatId = update.getMessage().getChatId();
        String ns = session.getActiveFlowNs(chatId);

        FlowDefinition def = registry.get(ns);
        if (def == null) return;

        engine.handle(update, def);
    }


    @Override
    public String getCommand() {
        return CommandName.UNKNOWN.getName(); // имя не важно, это внутренний роутер
    }
}
