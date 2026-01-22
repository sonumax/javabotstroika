package com.sonumax2.javabot.domain.draft.service;

import tools.jackson.databind.ObjectMapper;
import com.sonumax2.javabot.domain.draft.UserDraft;
import com.sonumax2.javabot.domain.draft.repo.UserDraftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DraftService {

    private static final Logger log = LoggerFactory.getLogger(DraftService.class);

    private final UserDraftRepository repo;
    private final ObjectMapper objectMapper;


    public DraftService(UserDraftRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
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
            log.warn("Failed to read draft json for type={}", type.getName(), e);
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
