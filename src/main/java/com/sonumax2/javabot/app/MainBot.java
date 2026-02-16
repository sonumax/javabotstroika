package com.sonumax2.javabot.app;

import com.sonumax2.javabot.bot.commands.CommandHandler;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.domain.auth.AuthService;
import com.sonumax2.javabot.domain.auth.UserRole;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

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
            BotUi botUi
    ) {
        this.botToken = botToken;
        this.commandHandler = commandHandler;
        this.userSessionService = userSessionService;
        this.authService = authService;
        this.botUi = botUi;
    }

    @Override public String getBotToken() { return botToken; }

    @Override public LongPollingUpdateConsumer getUpdatesConsumer() { return this; }

    @Override
    public void consume(Update update) {
        try {
            // 1) Сначала: админские callback-и approve/block
            if (tryHandleAuthCallback(update)) return;

            // 2) Потом: общий доступ (whitelist)
            Long chatId = extractChatId(update);
            if (chatId != null) {
                String firstName = extractFirstName(update);
                String username = extractUsername(update);

                var auth = authService.checkOrBootstrap(chatId, firstName, username);

                if (!auth.allowed()) {
                    // notifyAdmins == true только когда PENDING создан впервые (чтобы не спамить)
                    if (auth.notifyAdmins()) {
                        notifyAdminsAccessRequest(chatId, firstName, username);
                    }
                    if (auth.replyText() != null) botUi.sendText(chatId, auth.replyText());
                    return;
                }
            }

            // 3) Дальше всё как было
            userSessionService.touchFromUpdate(update);
            commandHandler.handle(update);

        } catch (Throwable e) {
            log.error("Unhandled error while processing update: {}", update, e);
        }
    }

    private boolean tryHandleAuthCallback(Update update) {
        if (!update.hasCallbackQuery() || update.getCallbackQuery() == null) return false;

        var cb = update.getCallbackQuery();
        var data = cb.getData();
        if (!AuthCb.isAuth(data)) return false;

        long adminChatId = cb.getMessage().getChatId();
        String adminFirstName = cb.getFrom() != null ? cb.getFrom().getFirstName() : null;
        String adminUsername = cb.getFrom() != null ? cb.getFrom().getUserName() : null;

        // Проверка, что нажимающий — реально админ
        var adminAuth = authService.checkOrBootstrap(adminChatId, adminFirstName, adminUsername);
        if (!adminAuth.allowed() || adminAuth.role() != UserRole.ADMIN) {
            botUi.sendText(adminChatId, "⛔ Нет прав.");
            return true;
        }

        long targetChatId = AuthCb.chatId(data);

        if (AuthCb.isApprove(data)) {
            authService.approve(targetChatId, adminChatId);
            botUi.sendText(adminChatId, "✅ Одобрено: " + targetChatId);
            botUi.sendText(targetChatId, "✅ Доступ одобрен. Можешь пользоваться ботом.");
            return true;
        }

        if (AuthCb.isBlock(data)) {
            authService.block(targetChatId, adminChatId);
            botUi.sendText(adminChatId, "⛔ Заблокирован: " + targetChatId);
            botUi.sendText(targetChatId, "⛔ Доступ запрещён.");
            return true;
        }

        return true;
    }

    private void notifyAdminsAccessRequest(long chatId, String firstName, String username) {
        String u = (username == null || username.isBlank()) ? "-" : "@" + username;
        String n = (firstName == null || firstName.isBlank()) ? "-" : firstName;

        String text = "🆕 Заявка на доступ\n"
                + "Имя: " + n + "\n"
                + "Username: " + u + "\n"
                + "chatId: " + chatId;

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        InlineKeyboardButton.builder().text("✅ Approve").callbackData(AuthCb.approve(chatId)).build(),
                        InlineKeyboardButton.builder().text("⛔ Block").callbackData(AuthCb.block(chatId)).build()
                )))
                .build();

        for (Long adminId : authService.adminChatIds()) {
            // нужен метод BotUi: sendText(chatId, text, kb)
            botUi.sendText(adminId, text, kb);
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

    // Вложенный helper — можно вынести в отдельный файл
    static final class AuthCb {
        static String approve(long chatId) { return "AUTH:APPROVE:" + chatId; }
        static String block(long chatId) { return "AUTH:BLOCK:" + chatId; }

        static boolean isAuth(String data) { return data != null && data.startsWith("AUTH:"); }
        static boolean isApprove(String data) { return data != null && data.startsWith("AUTH:APPROVE:"); }
        static boolean isBlock(String data) { return data != null && data.startsWith("AUTH:BLOCK:"); }

        static long chatId(String data) {
            return Long.parseLong(data.substring(data.lastIndexOf(':') + 1));
        }
    }
}
