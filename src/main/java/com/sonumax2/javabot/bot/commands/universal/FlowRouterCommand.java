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
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Order(1)
@Service
public class FlowRouterCommand implements Command {

    private enum Action { START_FROM_MESSAGE, HANDLE }

    private record Route(boolean handled, Action action, FlowDefinition<?> def) {}

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
        return tryRoute(update).handled();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handle(Update update) {
        Route r = tryRoute(update);
        if (!r.handled()) return;

        if (r.action() == Action.START_FROM_MESSAGE) {
            engine.startFromMessage(update, (FlowDefinition) r.def());
        } else {
            engine.handle(update, (FlowDefinition) r.def());
        }
    }

    private Route tryRoute(Update update) {
        if (update == null) return new Route(false, null, null);

        // 0) START COMMANDS: /advance, /expense, ...
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            if (text != null && text.startsWith("/")) {
                FlowDefinition<?> def = registry.getByCommand(text);
                return (def != null) ? new Route(true, Action.START_FROM_MESSAGE, def)
                        : new Route(false, null, null);
            }
        }

        // 1) CALLBACKS
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            String data = update.getCallbackQuery().getData();

            // 1a) стартовые колбеки разрешаем всегда
            FlowDefinition<?> defStart = registry.getByStartCallback(data);
            if (defStart != null) return new Route(true, Action.HANDLE, defStart);

            // 1b) остальные колбеки только если активен flow и ns совпадает
            String ns = extractNs(data);
            if (ns == null) return new Route(false, null, null);

            FlowDefinition<?> def = registry.get(ns);
            if (def == null) return new Route(false, null, null);

            MaybeInaccessibleMessage msg = update.getCallbackQuery().getMessage();
            if (msg == null) return new Route(false, null, null);
            long chatId = msg.getChatId();

            if (session.getUserState(chatId) != UserState.FLOW_WAIT_INPUT) return new Route(false, null, null);

            String activeNs = session.getActiveFlowNs(chatId);
            if (!ns.equals(activeNs)) return new Route(false, null, null);

            return new Route(true, Action.HANDLE, def);
        }

        // 2) MESSAGES: текст/фото только когда активен flow
        if (!update.hasMessage()) return new Route(false, null, null);

        Message msg = update.getMessage();
        long chatId = msg.getChatId();

        if (session.getUserState(chatId) != UserState.FLOW_WAIT_INPUT) return new Route(false, null, null);

        String ns = session.getActiveFlowNs(chatId);
        if (ns == null || ns.isBlank()) return new Route(false, null, null);

        FlowDefinition<?> def = registry.get(ns);
        if (def == null) return new Route(false, null, null);

        // текст внутри flow — только не-команда
        if (msg.hasText()) {
            String text = msg.getText();
            if (text == null || text.startsWith("/")) return new Route(false, null, null);
            return new Route(true, Action.HANDLE, def);
        }

        // файл/фото внутри flow
        if (msg.hasPhoto() || msg.hasDocument()) {
            return new Route(true, Action.HANDLE, def);
        }

        return new Route(false, null, null);
    }

    private String extractNs(String data) {
        if (data == null) return null;
        int p = data.indexOf(':');
        if (p <= 0) return null;
        return data.substring(0, p);
    }

    @Override
    public String getCommand() {
        return CommandName.UNKNOWN.getName(); // имя не важно, это внутренний роутер
    }
}
