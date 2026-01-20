package com.sonumax2.javabot.service;

import com.sonumax2.javabot.model.reference.Counterparty;
import com.sonumax2.javabot.model.reference.CounterpartyKind;
import com.sonumax2.javabot.model.repo.CounterpartyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
        return repo.findByActiveTrueOrderByNameAsc(PageRequest.of(0, 50));
    }

    public List<Counterparty> listActiveTop50(CounterpartyKind kind) {
        return repo.findByActiveTrueAndKindOrderByNameAsc(kindOrDefault(kind), PageRequest.of(0, 50));
    }

    public List<Counterparty> recentByChat(long chatId, int limit) {
        return repo.findByActiveTrueAndCreatedByChatIdOrderByCreatedAtDesc(chatId, PageRequest.of(0, limit));
    }

    public List<Counterparty> search(String rawName, int limit) {
        String ui = normalizeUi(rawName);
        if (ui.isBlank()) return List.of();
        return repo.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(ui, PageRequest.of(0, limit));
    }

    public List<Counterparty> search(CounterpartyKind kind, String rawName, int limit) {
        String ui = normalizeUi(rawName);
        if (ui.isBlank()) return List.of();
        return repo.findByActiveTrueAndKindAndNameContainingIgnoreCaseOrderByNameAsc(
                kindOrDefault(kind), ui, PageRequest.of(0, limit)
        );
    }


    public Optional<Counterparty> findExact(String raw) {
        String ui = normalizeUi(raw);
        if (ui.isBlank()) return Optional.empty();
        return repo.findFirstByActiveTrueAndNameNorm(normalizeNorm(ui));
    }

    public Optional<Counterparty> findExact(CounterpartyKind kind, String raw) {
        String ui = normalizeUi(raw);
        if (ui.isBlank()) return Optional.empty();
        return repo.findFirstByActiveTrueAndKindAndNameNorm(kindOrDefault(kind), normalizeNorm(ui));
    }

    public Counterparty getOrCreate(String rawName, CounterpartyKind kind, long chatId) {
        String ui = normalizeUi(rawName);
        if (ui.isBlank()) throw new IllegalArgumentException("counterparty name is blank");

        CounterpartyKind k = kindOrDefault(kind);
        String norm = normalizeNorm(ui);

        // 1) Уникальность по active+name_norm без kind -> ищем так
        Optional<Counterparty> active = repo.findFirstByActiveTrueAndNameNorm(norm);
        if (active.isPresent()) {
            Counterparty cp = active.get();
            // мягкий апгрейд kind: если было OTHER, а теперь уточнили — обновим
            if (cp.getKind() == CounterpartyKind.OTHER && k != CounterpartyKind.OTHER) {
                cp.setKind(k);
                return repo.save(cp);
            }
            return cp;
        }

        // 2) Если есть неактивный — реактивируем (берём самый свежий дубль)
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
                    // если параллельно активировали/создали — возвращаем активный
                    return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> e);
                }
            }
        }

        // 3) Создаём новый
        Counterparty cp = new Counterparty();
        cp.setName(ui);
        cp.setNameNorm(norm);
        cp.setKind(k);
        cp.setActive(true);
        cp.setCreatedByChatId(chatId);
        cp.setCreatedAt(Instant.now()); // как в NomenclatureService (Spring Data JDBC)

        try {
            return repo.save(cp);
        } catch (DataIntegrityViolationException e) {
            // если параллельно создали — забираем существующий
            return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> e);
        }
    }

    public Counterparty getOrCreate(String rawName, long chatId) {
        return getOrCreate(rawName, CounterpartyKind.OTHER, chatId);
    }

    private static CounterpartyKind kindOrDefault(CounterpartyKind kind) {
        return kind == null ? CounterpartyKind.OTHER : kind;
    }

    public static String normalizeUi(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    public static String normalizeNorm(String s) {
        return normalizeUi(s).toLowerCase(Locale.ROOT);
    }
}
