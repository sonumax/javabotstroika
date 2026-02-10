package com.sonumax2.javabot.domain.reference.service;

import com.sonumax2.javabot.domain.reference.FuelMachineType;
import com.sonumax2.javabot.domain.reference.repo.FuelMachineTypeRepository;
import com.sonumax2.javabot.util.NameNormUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FuelMachineTypeService {

    private final FuelMachineTypeRepository repo;

    public FuelMachineTypeService(FuelMachineTypeRepository repo) {
        this.repo = repo;
    }

    public List<FuelMachineType> listActive(long chatId) {
        return repo.findActive(chatId);
    }

    public List<FuelMachineType> search(long chatId, String query) {
        String norm = NameNormUtils.normalizeNorm(query);
        return repo.search(chatId, "%" + norm + "%");
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