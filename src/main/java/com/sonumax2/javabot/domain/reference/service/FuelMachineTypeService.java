package com.sonumax2.javabot.domain.reference.service;

import com.sonumax2.javabot.domain.reference.FuelMachineType;
import com.sonumax2.javabot.domain.reference.repo.FuelMachineTypeRepository;
import com.sonumax2.javabot.util.NameNormUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class FuelMachineTypeService {

    private final FuelMachineTypeRepository repo;

    public FuelMachineTypeService(FuelMachineTypeRepository repo) {
        this.repo = repo;
    }

    public Optional<FuelMachineType> findActiveById(Long id) {
        if (id == null) return Optional.empty();
        return repo.findById(id).filter(FuelMachineType::isActive);
    }

    public List<FuelMachineType> listActive() {
        return repo.findByActiveTrueOrderByNameAsc();
    }

    public List<FuelMachineType> listActiveTop50() {
        return repo.activeList(50);
    }

    public List<FuelMachineType> recentByChat(long chatId, int limit) {
        return repo.recentCreatedByChat(chatId, limit);
    }

    public List<FuelMachineType> suggestByChat(long chatId, int limit) {
        if (limit <= 0) return List.of();

        List<FuelMachineType> recent = recentByChat(chatId, limit);
        List<FuelMachineType> fallback = listActiveTop50();

        ArrayList<FuelMachineType> out = new ArrayList<>(limit);
        Set<Long> seen = new HashSet<>();

        for (FuelMachineType e : recent) {
            if (e == null || !e.isActive()) continue;
            if (e.getId() != null && seen.add(e.getId())) {
                out.add(e);
                if (out.size() >= limit) return out;
            }
        }

        for (FuelMachineType e : fallback) {
            if (e == null || !e.isActive()) continue;
            if (e.getId() != null && seen.add(e.getId())) {
                out.add(e);
                if (out.size() >= limit) return out;
            }
        }

        return out;
    }

    public List<FuelMachineType> search(String rawName, int limit) {
        String norm = NameNormUtils.normalizeNorm(rawName);
        if (norm.isBlank()) return List.of();
        return repo.searchActiveByName(norm, limit);
    }

    public Optional<FuelMachineType> findExact(String raw) {
        String ui = NameNormUtils.normalizeUi(raw);
        if (ui.isBlank()) return Optional.empty();
        return repo.findFirstByActiveTrueAndNameNorm(NameNormUtils.normalizeNorm(ui));
    }

    public FuelMachineType getOrCreate(String rawName, long chatId) {
        String ui = NameNormUtils.normalizeUi(rawName);
        if (ui.isBlank()) throw new IllegalArgumentException("fuel_machine_type name is blank");

        String norm = NameNormUtils.normalizeNorm(ui);

        // 1) уже есть активный
        Optional<FuelMachineType> active = repo.findFirstByActiveTrueAndNameNorm(norm);
        if (active.isPresent()) return active.get();

        // 2) если есть неактивный — реактивируем самый свежий
        Optional<FuelMachineType> any = repo.findTop1ByNameNormOrderByIdDesc(norm);
        if (any.isPresent()) {
            FuelMachineType e = any.get();
            if (!e.isActive()) {
                e.setActive(true);
                e.setName(ui);
                e.setNameNorm(norm);
                try {
                    return repo.save(e);
                } catch (DataIntegrityViolationException ex) {
                    return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> ex);
                }
            }
        }

        // 3) создаём новый
        FuelMachineType e = new FuelMachineType();
        e.setName(ui);
        e.setNameNorm(norm);
        e.setActive(true);
        e.setCreatedByChatId(chatId);
        e.setCreatedAt(Instant.now());

        try {
            return repo.save(e);
        } catch (DataIntegrityViolationException ex) {
            return repo.findFirstByActiveTrueAndNameNorm(norm).orElseThrow(() -> ex);
        }
    }

    public Optional<String> findName(long id) {
        return findActiveById(id).map(FuelMachineType::getName);
    }
}
