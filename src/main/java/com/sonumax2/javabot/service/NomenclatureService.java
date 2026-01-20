package com.sonumax2.javabot.service;

import com.sonumax2.javabot.model.reference.Nomenclature;
import com.sonumax2.javabot.model.repo.NomenclatureRepository;
import com.sonumax2.javabot.util.NameNormUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class NomenclatureService {

    private final NomenclatureRepository repo;

    public NomenclatureService(NomenclatureRepository repo) {
        this.repo = repo;
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
}
