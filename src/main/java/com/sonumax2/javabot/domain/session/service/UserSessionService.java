package com.sonumax2.javabot.domain.session.service;

import com.sonumax2.javabot.domain.session.UserSession;
import com.sonumax2.javabot.domain.session.UserState;
import com.sonumax2.javabot.domain.session.repo.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserSessionService {
    private final UserSessionRepository userSessionRepository;

    public UserSessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    private UserSession getOnCreateUserSession(long chatId) {
        Optional<UserSession> userSession = userSessionRepository.findByChatId(chatId);
        return userSession.orElseGet(() -> {
            UserSession newSession = new UserSession();
            newSession.setChatId(chatId);
            newSession.setUserState(UserState.IDLE);
            newSession.setLocale("ru");
            return userSessionRepository.save(newSession);
        });
    }

    public UserState getUserState(long chatId) {
        return getOnCreateUserSession(chatId).getUserState();
    }

    public Locale getLocale(long chatId) {
        return Locale.forLanguageTag(getOnCreateUserSession(chatId).getLocale());
    }

    public void setLocale(long chatId, String locale) {
        UserSession userSession = getOnCreateUserSession(chatId);
        userSession.setLocale(locale);
        userSessionRepository.save(userSession);
    }

    public void setUserState(long chatId, UserState userState) {
        UserSession userSession = getOnCreateUserSession(chatId);
        userSession.setUserState(userState);
        userSessionRepository.save(userSession);
    }

    public String displayName(long chatId) {
        UserSession s = getOnCreateUserSession(chatId);

        if (s.getFirstName() != null && !s.getFirstName().isBlank()) return s.getFirstName().trim();

        if (s.getUsername() != null && !s.getUsername().isBlank()) return "@" + s.getUsername().trim();

        return "друг";
    }

    public void touchFromUpdate(Update update) {
        long chatId = extractChatId(update);
        if (chatId == 0) return;

        User tgUser = extractUser(update);
        if (tgUser == null) return;

        UserSession s = getOnCreateUserSession(chatId);
        s.setFirstName(buildDisplayName(tgUser));
        s.setUsername(normalizeUsername(tgUser.getUserName()));
        userSessionRepository.save(s);
    }

    private long extractChatId(Update u) {
        if (u == null) return 0;
        if (u.hasMessage() && u.getMessage().getChat() != null) {
            return u.getMessage().getChatId();
        }
        if (u.hasCallbackQuery()
                && u.getCallbackQuery().getMessage() != null
                && u.getCallbackQuery().getMessage().getChat() != null) {
            return u.getCallbackQuery().getMessage().getChatId();
        }
        return 0;
    }

    private User extractUser(Update u) {
        if (u == null) return null;
        if (u.hasMessage()) return u.getMessage().getFrom();
        if (u.hasCallbackQuery()) return u.getCallbackQuery().getFrom();
        return null;
    }

    private String buildDisplayName(User u) {
        String fn = safe(u.getFirstName());
        String ln = safe(u.getLastName());
        String un = safe(u.getUserName());

        String name = (fn + " " + ln).trim();
        if (!name.isBlank()) return cut(name, 128);

        if (!un.isBlank()) return cut("@" + un, 128);
        return "User";
    }

    private String normalizeUsername(String username) {
        if (username == null) return null;
        String u = username.trim();
        if (u.isBlank()) return null;
        if (u.startsWith("@")) u = u.substring(1);
        return cut(u, 64);
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
