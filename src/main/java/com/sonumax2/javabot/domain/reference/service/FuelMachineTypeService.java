package com.sonumax2.javabot.domain.reference.service;

import com.sonumax2.javabot.domain.reference.FuelMachineType;
import com.sonumax2.javabot.domain.reference.repo.FuelMachineTypeRepository;
import com.sonumax2.javabot.util.NameNormUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FuelMachineTypeService {

    private final FuelMachineTypeRepository repo;

    public FuelMachineTypeService(FuelMachineTypeRepository repo) {
        this.repo = repo;
    }

    public List<FuelMachineType> listActive(long chatId) {
        return repo.findActive(chatId);
    }

    public Optional<FuelMachineType> findExact(long chatId, String text) {
        String norm = NameNormUtils.normalizeNorm(text);
        if (norm.isBlank()) return Optional.empty();
        return repo.findByNorm(chatId, norm);
    }

    public List<FuelMachineType> search(long chatId, String query, int limit) {
        String norm = NameNormUtils.normalizeNorm(query);
        if (norm.isBlank()) return List.of();
        List<FuelMachineType> found = repo.search(chatId, "%" + norm + "%");
        if (limit <= 0 || found.size() <= limit) return found;
        return found.subList(0, limit);
    }

    public Optional<String> findName(long chatId, Long id) {
        if (id == null) return Optional.empty();
        return repo.findById(id)
                .filter(FuelMachineType::isActive)
                .filter(x -> x.getChatId() != null && x.getChatId() == chatId)
                .map(FuelMachineType::getName);
    }

    public List<FuelMachineType> search(long chatId, String query) {
        return search(chatId, query, 20);
    }

    public FuelMachineType getOrCreate(long chatId, String name) {
        String norm = NameNormUtils.normalizeNorm(name);

        return repo.findByNorm(chatId, norm)
                .orElseGet(() -> {
                    FuelMachineType t = new FuelMachineType();
                    t.setChatId(chatId);
                    t.setName(name.trim());
                    t.setNameNorm(norm);
                    t.setActive(true);
                    t.setSortOrder(0);
                    t.setCreatedAt(LocalDateTime.now());
                    return repo.save(t);
                });
    }
}