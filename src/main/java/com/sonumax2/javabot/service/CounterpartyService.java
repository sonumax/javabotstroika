package com.sonumax2.javabot.service;

import com.sonumax2.javabot.model.reference.Counterparty;
import com.sonumax2.javabot.model.reference.CounterpartyKind;
import com.sonumax2.javabot.model.repo.CounterpartyRepository;
import com.sonumax2.javabot.util.NameNormUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CounterpartyService {

    private final CounterpartyRepository repo;

    public CounterpartyService(CounterpartyRepository repo) {
        this.repo = repo;
    }

    public List<Counterparty> listActive() {
        return repo.findByActiveTrueOrderByNameAsc();
    }

    public List<Counterparty> listActive(CounterpartyKind kind) {
        return repo.findByActiveTrueAndKindOrderByNameAsc(kindOrDefault(kind));
    }

    public List<Counterparty> listActiveTop50() {
        return repo.activeList(50);
    }

    public List<Counterparty> listActiveTop50(CounterpartyKind kind) {
        return repo.activeListByKind(kindOrDefault(kind), 50);
    }

    public List<Counterparty> recentByChat(long chatId, int limit) {
        return repo.recentCreatedByChat(chatId, limit);
    }

    public List<Counterparty> search(String rawName, int limit) {
        String ui = NameNormUtils.normalizeUi(rawName);
        if (ui.isBlank()) return List.of();
        return repo.searchActiveByName(ui, limit);
    }

    public List<Counterparty> search(CounterpartyKind kind, String rawName, int limit) {
        String ui = NameNormUtils.normalizeUi(rawName);
        if (ui.isBlank()) return List.of();
        return repo.searchActiveByKindAndName(kindOrDefault(kind), ui, limit);
    }

    public Optional<Counterparty> findExact(String raw) {
        String ui = NameNormUtils.normalizeUi(raw);
        if (ui.isBlank()) return Optional.empty();
        return repo.findFirstByActiveTrueAndNameNorm(NameNormUtils.normalizeNorm(ui));
    }

    public Optional<Counterparty> findExact(CounterpartyKind kind, String raw) {
        String ui = NameNormUtils.normalizeUi(raw);
        if (ui.isBlank()) return Optional.empty();
        return repo.findFirstByActiveTrueAndKindAndNameNorm(kindOrDefault(kind), NameNormUtils.normalizeNorm(ui));
    }

    public Counterparty getOrCreate(String rawName, CounterpartyKind kind, long chatId) {
        String ui = NameNormUtils.normalizeUi(rawName);
        if (ui.isBlank()) throw new IllegalArgumentException("counterparty name is blank");

        CounterpartyKind k = kindOrDefault(kind);
        String norm = NameNormUtils.normalizeNorm(ui);

        // 1) уникальность среди активных по name_norm (без kind)
        Optional<Counterparty> active = repo.findFirstByActiveTrueAndNameNorm(norm);
        if (active.isPresent()) {
            Counterparty cp = active.get();
            // мягкий апгрейд kind
            if (cp.getKind() == CounterpartyKind.OTHER && k != CounterpartyKind.OTHER) {
                cp.setKind(k);
                return repo.save(cp);
            }
            return cp;
        }

        // 2) если есть неактивный — реактивируем самый свежий дубль
        Optional<Counterparty> any = repo.findTop1ByNameNormOrderByIdDesc(norm);
        if (any.isPresent()) {
            Counterparty cp = any.get();
            if (!cp.isActive()) {
                cp.setActive(true);
                cp.setName(ui);
                cp.setKind(k);
                try {
                    return repo.save(cp);
                } catch (DataIntegrityViolationException e) {
                    return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> e);
                }
            }
        }

        // 3) создаём новый
        Counterparty cp = new Counterparty();
        cp.setName(ui);
        cp.setNameNorm(norm);
        cp.setKind(k);
        cp.setActive(true);
        cp.setCreatedByChatId(chatId);
        cp.setCreatedAt(Instant.now());

        try {
            return repo.save(cp);
        } catch (DataIntegrityViolationException e) {
            return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> e);
        }
    }

    public Counterparty getOrCreate(String rawName, long chatId) {
        return getOrCreate(rawName, CounterpartyKind.OTHER, chatId);
    }

    private static CounterpartyKind kindOrDefault(CounterpartyKind kind) {
        return kind == null ? CounterpartyKind.OTHER : kind;
    }
}
