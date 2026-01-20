package com.sonumax2.javabot.service;

import com.sonumax2.javabot.model.reference.Nomenclature;
import com.sonumax2.javabot.model.repo.NomenclatureRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
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

    public List<Nomenclature> recentByChat(long chatId, int limit) {
        return repo.findByActiveTrueAndCreatedByChatIdOrderByCreatedAtDesc(chatId, PageRequest.of(0, limit));
    }

    public List<Nomenclature> search(String rawName, int limit) {
        String ui = normalizeUi(rawName);
        if (ui.isBlank()) return List.of();
        return repo.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(ui, PageRequest.of(0, limit));
    }

    public Nomenclature getOrCreate(String rawName, long chatId) {
        String ui = normalizeUi(rawName);
        if (ui.isBlank()) {
            throw new IllegalArgumentException("nomenclature name is blank");
        }

        String norm = normalizeNorm(ui);

        return repo.findFirstByActiveTrueAndNameNorm(norm).orElseGet(() -> {
            Nomenclature n = new Nomenclature();
            n.setName(ui);
            n.setNameNorm(norm);
            n.setActive(true);
            n.setCreatedByChatId(chatId);
            n.setCreatedAt(Instant.now()); // надёжно для Spring Data JDBC

            try {
                return repo.save(n);
            } catch (DataIntegrityViolationException e) {
                // если параллельно создали — забираем существующий по name_norm
                return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> e);
            }
        });
    }

    public Optional<Nomenclature> findExact(String raw) {
        String ui = normalizeUi(raw);
        if (ui.isBlank()) return Optional.empty();
        return repo.findFirstByActiveTrueAndNameNorm(normalizeNorm(ui));
    }

    public List<Nomenclature> searchSimple(String raw) {
        String ui = normalizeUi(raw);
        if (ui.isBlank()) return List.of();
        return repo.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(ui, PageRequest.of(0, 8));
    }

    public List<Nomenclature> listActiveTop50() {
        return repo.findByActiveTrueOrderByNameAsc(PageRequest.of(0, 50));
    }

    public static String normalizeUi(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    public static String normalizeNorm(String s) {
        return normalizeUi(s).toLowerCase(Locale.ROOT);
    }
}
