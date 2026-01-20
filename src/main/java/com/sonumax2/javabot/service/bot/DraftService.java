package com.sonumax2.javabot.service.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonumax2.javabot.model.user.UserDraft;
import com.sonumax2.javabot.model.repo.UserDraftRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DraftService {

    private final UserDraftRepository repo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DraftService(UserDraftRepository repo) {
        this.repo = repo;
        objectMapper.findAndRegisterModules();
    }

    public <T> Optional<T> find(Long chatId, String draftType, Class<T> type) {
        return repo.findByChatIdAndDraftType(chatId, draftType)
                .flatMap(d -> safeRead(d.getDataJson(), type));
    }

    /** get-or-create */
    public <T> T get(Long chatId, String draftType, Class<T> type) {
        UserDraft entity = repo.findByChatIdAndDraftType(chatId, draftType)
                .orElseGet(() -> {
                    UserDraft d = new UserDraft();
                    d.setChatId(chatId);
                    d.setDraftType(draftType);
                    d.setDataJson("{}");
                    d.setUpdatedAt(LocalDateTime.now());
                    return d;
                });

        return safeRead(entity.getDataJson(), type).orElseGet(() -> newInstance(type));
    }

    public <T> void save(Long chatId, String draftType, T draft) {
        UserDraft entity = repo.findByChatIdAndDraftType(chatId, draftType)
                .orElseGet(() -> {
                    UserDraft d = new UserDraft();
                    d.setChatId(chatId);
                    d.setDraftType(draftType);
                    return d;
                });

        entity.setDataJson(writeJson(draft));
        entity.setUpdatedAt(LocalDateTime.now());
        repo.save(entity);
    }

    public void clear(Long chatId, String draftType) {
        repo.deleteByChatIdAndDraftType(chatId, draftType);
    }

    public void clearAll(Long chatId) {
        repo.deleteByChatId(chatId);
    }

    /* ---------- helpers ---------- */

    private <T> Optional<T> safeRead(String json, Class<T> type) {
        try {
            return Optional.ofNullable(objectMapper.readValue(json, type));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize draft to JSON", e);
        }
    }

    private <T> T newInstance(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Draft type must have no-args constructor: " + type.getName(), e);
        }
    }
}
