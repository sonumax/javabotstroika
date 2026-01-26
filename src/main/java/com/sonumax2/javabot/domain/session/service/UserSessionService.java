package com.sonumax2.javabot.domain.session.service;

import com.sonumax2.javabot.domain.session.UserSession;
import com.sonumax2.javabot.domain.session.UserState;
import com.sonumax2.javabot.domain.session.repo.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;

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
            newSession.setTimezone("Europe/Minsk");
            return userSessionRepository.save(newSession);
        });
    }

    public UserState getUserState(long chatId) {
        return getOnCreateUserSession(chatId).getUserState();
    }

    public Locale getLocale(long chatId) {
        String tag = getOnCreateUserSession(chatId).getLocale();
        if (tag == null || tag.isBlank()) tag = "ru";
        return Locale.forLanguageTag(tag);
    }

    public void setLocale(long chatId, String locale) {
        int updated = userSessionRepository.updateLocale(chatId, locale);
        if (updated > 0) return;

        ensureExists(chatId);
        userSessionRepository.updateLocale(chatId, locale);
    }

    public void setUserState(long chatId, UserState userState) {
        String state = userState.name();

        int updated = userSessionRepository.updateUserState(chatId, state);
        if (updated > 0) return;

        ensureExists(chatId);
        userSessionRepository.updateUserState(chatId, state);
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

        String newFirstName = buildDisplayName(tgUser);
        String newUsername = normalizeUsername(tgUser.getUserName());

        UserSession s = getOnCreateUserSession(chatId);

        boolean changed = false;

        if (!Objects.equals(s.getFirstName(), newFirstName)) {
            s.setFirstName(newFirstName);
            changed = true;
        }

        if (!Objects.equals(s.getUsername(), newUsername)) {
            s.setUsername(newUsername);
            changed = true;
        }

        if (changed) {
            userSessionRepository.save(s);
        }
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

    public Long getPanelMessageId(long chatId) {
        return getOnCreateUserSession(chatId).getPanelMessageId();
    }

    public void setPanelMessageId(long chatId, Integer messageId) {
        Long newVal = (messageId == null) ? null : messageId.longValue();

        int updated = userSessionRepository.updatePanelId(chatId, newVal);
        if (updated > 0) return;

        ensureExists(chatId);
        userSessionRepository.updatePanelId(chatId, newVal);
    }

    public void clearPanelMessageId(long chatId) {
        setPanelMessageId(chatId, null);
    }

    public String getActiveFlowNs(long chatId) {
        return getOnCreateUserSession(chatId).getActiveFlowNs();
    }

    public String getActiveDraftType(long chatId) {
        return getOnCreateUserSession(chatId).getActiveDraftType();
    }

    public void setActiveFlow(long chatId, String ns, String draftType) {
        int updated = userSessionRepository.updateActiveFlow(chatId, ns, draftType);
        if (updated > 0) return;

        ensureExists(chatId);
        userSessionRepository.updateActiveFlow(chatId, ns, draftType);
    }

    public void clearActiveFlow(long chatId) {
        int updated = userSessionRepository.clearActiveFlow(chatId);
        if (updated > 0) return;

        ensureExists(chatId);
        userSessionRepository.clearActiveFlow(chatId);
    }

    private void ensureExists(long chatId) {
        userSessionRepository.findByChatId(chatId).orElseGet(() -> {
            UserSession s = new UserSession();
            s.setChatId(chatId);
            s.setUserState(UserState.IDLE);
            s.setLocale("ru");
            s.setTimezone("Europe/Minsk");
            return userSessionRepository.save(s);
        });
    }

    public ZoneId getZoneId(long chatId) {
        String tz = getOnCreateUserSession(chatId).getTimezone();
        if (tz == null || tz.isBlank()) return ZoneId.of("Europe/Minsk");
        try { return ZoneId.of(tz); }
        catch (Exception e) { return ZoneId.of("Europe/Minsk"); }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
