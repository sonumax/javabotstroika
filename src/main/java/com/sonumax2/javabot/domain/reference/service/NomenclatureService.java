package com.sonumax2.javabot.domain.reference.service;

import com.sonumax2.javabot.domain.reference.Nomenclature;
import com.sonumax2.javabot.domain.reference.repo.NomenclatureRepository;
import com.sonumax2.javabot.util.NameNormUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class NomenclatureService {

    private final NomenclatureRepository repo;

    public NomenclatureService(NomenclatureRepository repo) {
        this.repo = repo;
    }

    public Optional<Nomenclature> findActiveById(Long id) {
        if (id == null) return Optional.empty();
        return repo.findById(id).filter(Nomenclature::isActive);
    }

    public List<Nomenclature> listActive() {
        return repo.findByActiveTrueOrderByNameAsc();
    }

    public List<Nomenclature> listActiveTop50() {
        return repo.activeList(50);
    }

    public List<Nomenclature> recentByChat(long chatId, int limit) {
        return repo.recentCreatedByChat(chatId, limit);
    }

    public List<Nomenclature> suggestByChat(long chatId, int limit) {
        if (limit <= 0) return List.of();

        List<Nomenclature> recent = recentByChat(chatId, limit);
        List<Nomenclature> fallback = listActiveTop50();

        ArrayList<Nomenclature> out = new ArrayList<>(limit);
        Set<Long> seen = new HashSet<>();

        for (Nomenclature n : recent) {
            if (n == null || !n.isActive()) continue;
            if (n.getId() != null && seen.add(n.getId())) {
                out.add(n);
                if (out.size() >= limit) return out;
            }
        }

        for (Nomenclature n : fallback) {
            if (n == null || !n.isActive()) continue;
            if (n.getId() != null && seen.add(n.getId())) {
                out.add(n);
                if (out.size() >= limit) return out;
            }
        }

        return out;
    }

    public List<Nomenclature> search(String rawName, int limit) {
        String ui = NameNormUtils.normalizeUi(rawName);
        if (ui.isBlank()) return List.of();
        return repo.searchActiveByName(ui, limit);
    }

    public List<Nomenclature> searchSimple(String raw) {
        String ui = NameNormUtils.normalizeUi(raw);
        if (ui.isBlank()) return List.of();
        return repo.searchActiveByName(ui, 8);
    }

    public Optional<Nomenclature> findExact(String raw) {
        String ui = NameNormUtils.normalizeUi(raw);
        if (ui.isBlank()) return Optional.empty();
        return repo.findFirstByActiveTrueAndNameNorm(NameNormUtils.normalizeNorm(ui));
    }

    public Nomenclature getOrCreate(String rawName, long chatId) {
        String ui = NameNormUtils.normalizeUi(rawName);
        if (ui.isBlank()) throw new IllegalArgumentException("nomenclature name is blank");

        String norm = NameNormUtils.normalizeNorm(ui);

        return repo.findFirstByActiveTrueAndNameNorm(norm).orElseGet(() -> {
            Nomenclature n = new Nomenclature();
            n.setName(ui);
            n.setNameNorm(norm);
            n.setActive(true);
            n.setCreatedByChatId(chatId);
            n.setCreatedAt(Instant.now());

            try {
                return repo.save(n);
            } catch (DataIntegrityViolationException e) {
                return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> e);
            }
        });
    }

    public List<Nomenclature> loadActiveInOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        LinkedHashSet<Long> set = new LinkedHashSet<>(ids);

        List<Nomenclature> items = StreamSupport
                .stream(repo.findAllById(set).spliterator(), false)
                .filter(Nomenclature::isActive)
                .toList();

        Map<Long, Nomenclature> map = new HashMap<>();
        for (Nomenclature n : items) map.put(n.getId(), n);

        List<Nomenclature> ordered = new ArrayList<>(set.size());
        for (Long id : set) {
            Nomenclature n = map.get(id);
            if (n != null) ordered.add(n);
        }

        return ordered;
    }
}
