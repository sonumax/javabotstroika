package com.sonumax2.javabot.bot.ui;

import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LocalizationService {
    private final UserSessionService userSessionService;
    private final MessageSource messageSource;

    public LocalizationService(UserSessionService userSessionService, MessageSource messageSource) {
        this.userSessionService = userSessionService;
        this.messageSource = messageSource;
    }

    public String getLocaleMessage(long chatId, String key, Object... args) {
        if (key == null || key.isBlank()) return "??null_key??";

        Locale locale = userSessionService.getLocale(chatId);
        if (locale == null) locale = Locale.getDefault();

        return messageSource.getMessage(key, args, "??" + key + "??", locale);
    }

    public String getLocaleMessage(long chatId, String key) {
        return getLocaleMessage(chatId, key, (Object[]) null);
    }
}
