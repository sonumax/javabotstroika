package com.sonumax2.javabot.domain.reference.service;

import com.sonumax2.javabot.domain.reference.WorkObject;
import com.sonumax2.javabot.domain.reference.repo.WorkObjectRepository;
import com.sonumax2.javabot.util.NameNormUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class WorkObjectService {

    private final WorkObjectRepository repo;

    public WorkObjectService(WorkObjectRepository repo) {
        this.repo = repo;
    }

    public Optional<WorkObject> findActiveById(Long id) {
        if (id == null) return Optional.empty();
        return repo.findById(id).filter(WorkObject::isActive);
    }

    public List<WorkObject> listActive() {
        return repo.findByActiveTrueOrderByNameAsc();
    }

    public List<WorkObject> listActiveTop50() {
        return repo.activeList(50);
    }

    public List<WorkObject> recentByChat(long chatId, int limit) {
        return repo.recentCreatedByChat(chatId, limit);
    }

    public List<WorkObject> suggestByChat(long chatId, int limit) {
        if (limit <= 0) return List.of();

        List<WorkObject> recent = recentByChat(chatId, limit);
        List<WorkObject> fallback = listActiveTop50();

        ArrayList<WorkObject> out = new ArrayList<>(limit);
        Set<Long> seen = new HashSet<>();

        for (WorkObject wo : recent) {
            if (wo == null || !wo.isActive()) continue;
            if (wo.getId() != null && seen.add(wo.getId())) {
                out.add(wo);
                if (out.size() >= limit) return out;
            }
        }

        for (WorkObject wo : fallback) {
            if (wo == null || !wo.isActive()) continue;
            if (wo.getId() != null && seen.add(wo.getId())) {
                out.add(wo);
                if (out.size() >= limit) return out;
            }
        }

        return out;
    }

    public List<WorkObject> search(String rawName, int limit) {
        String ui = NameNormUtils.normalizeUi(rawName);
        if (ui.isBlank()) return List.of();
        return repo.searchActiveByName(ui, limit);
    }

    public Optional<WorkObject> findExact(String raw) {
        String ui = NameNormUtils.normalizeUi(raw);
        if (ui.isBlank()) return Optional.empty();
        return repo.findFirstByActiveTrueAndNameNorm(NameNormUtils.normalizeNorm(ui));
    }

    public WorkObject getOrCreate(String rawName, long chatId) {
        String ui = NameNormUtils.normalizeUi(rawName);
        if (ui.isBlank()) throw new IllegalArgumentException("work_object name is blank");

        String norm = NameNormUtils.normalizeNorm(ui);

        // 1) уже есть активный
        Optional<WorkObject> active = repo.findFirstByActiveTrueAndNameNorm(norm);
        if (active.isPresent()) return active.get();

        // 2) если есть неактивный — реактивируем самый свежий
        Optional<WorkObject> any = repo.findTop1ByNameNormOrderByIdDesc(norm);
        if (any.isPresent()) {
            WorkObject wo = any.get();
            if (!wo.isActive()) {
                wo.setActive(true);
                wo.setName(ui);
                wo.setNameNorm(norm);
                try {
                    return repo.save(wo);
                } catch (DataIntegrityViolationException e) {
                    return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> e);
                }
            }
        }

        // 3) создаём новый
        WorkObject wo = new WorkObject();
        wo.setName(ui);
        wo.setNameNorm(norm);
        wo.setActive(true);
        wo.setCreatedByChatId(chatId);
        wo.setCreatedAt(Instant.now());

        try {
            return repo.save(wo);
        } catch (DataIntegrityViolationException e) {
            return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> e);
        }
    }
}
