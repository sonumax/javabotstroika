package com.sonumax2.javabot.service;

import com.sonumax2.javabot.model.reference.WorkObject;
import com.sonumax2.javabot.model.repo.WorkObjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class WorkObjectService {
    private final WorkObjectRepository repo;

    public WorkObjectService(WorkObjectRepository repo) {
        this.repo = repo;
    }

    public List<WorkObject> listActive() {
        return repo.findByActiveTrueOrderByNameAsc();
    }

    public Optional<WorkObject> findExact(String raw) {
        String ui = normalizeUi(raw);
        if (ui.isBlank()) return Optional.empty();
        return repo.findFirstByActiveTrueAndNameNorm(normalizeNorm(ui));
    }

    public WorkObject getOrCreate(String rawName, long chatId) {
        String ui = normalizeUi(rawName);
        if (ui.isBlank()) throw new IllegalArgumentException("work_object name is blank");

        String norm = normalizeNorm(ui);

        return repo.findFirstByActiveTrueAndNameNorm(norm).orElseGet(() -> {
            WorkObject wo = new WorkObject();
            wo.setName(ui);
            wo.setNameNorm(norm); // теперь в BaseRefEntity
            wo.setActive(true);
            wo.setCreatedByChatId(chatId);
            wo.setCreatedAt(Instant.now());

            try {
                return repo.save(wo);
            } catch (DataIntegrityViolationException e) {
                return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> e);
            }
        });
    }

    private static String normalizeUi(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNorm(String s) {
        return normalizeUi(s).toLowerCase(Locale.ROOT);
    }
}
