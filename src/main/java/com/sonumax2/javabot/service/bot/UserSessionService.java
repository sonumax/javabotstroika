package com.sonumax2.javabot.service.bot;

import com.sonumax2.javabot.model.user.UserSession;
import com.sonumax2.javabot.model.UserState;
import com.sonumax2.javabot.model.repo.UserSessionRepository;
import org.springframework.stereotype.Service;

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
}
