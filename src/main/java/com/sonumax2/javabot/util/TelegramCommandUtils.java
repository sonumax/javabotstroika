package com.sonumax2.javabot.util;

import com.sonumax2.javabot.domain.reference.BaseRefEntity;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public final class TelegramCommandUtils {
    private TelegramCommandUtils() {}

    /** Возвращает команду без "/" и без "@BotName": start/help/about и т.д. */
    public static Optional<String> extractCommand(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return Optional.empty();

        var msg = update.getMessage();
        String text = msg.getText();
        if (text == null || text.isBlank()) return Optional.empty();

        if (msg.getEntities() != null) {
            for (MessageEntity e : msg.getEntities()) {
                if (!"bot_command".equals(e.getType())) continue;
                if (e.getOffset() != 0) continue;
                String raw = text.substring(0, e.getLength()); // "/start" или "/start@Bot"
                return Optional.of(normalize(raw));
            }
        }

        if (text.startsWith("/")) {
            String raw = text.split("\\s+", 2)[0]; // "/start@Bot"
            return Optional.of(normalize(raw));
        }

        return Optional.empty();
    }

    private static String normalize(String raw) {
        String s = raw.startsWith("/") ? raw.substring(1) : raw;
        int at = s.indexOf('@');
        if (at >= 0) s = s.substring(0, at);
        return s.toLowerCase();
    }

    public static <T extends BaseRefEntity> List<T> mergeRecentWithActive(
            List<? extends T> recent,
            List<? extends T> active,
            int limit
    ) {
        LinkedHashMap<Long, T> out = new LinkedHashMap<>();

        for (T r : recent) {
            if (r != null && r.getId() != null) out.putIfAbsent(r.getId(), r);
            if (out.size() >= limit) break;
        }

        if (out.size() < limit) {
            for (T a : active) {
                if (a != null && a.getId() != null) out.putIfAbsent(a.getId(), a);
                if (out.size() >= limit) break;
            }
        }

        return new ArrayList<>(out.values());
    }
}
