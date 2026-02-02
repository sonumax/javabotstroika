package com.sonumax2.javabot.domain.reference.service;

import com.sonumax2.javabot.domain.reference.Equipment;
import com.sonumax2.javabot.domain.reference.repo.EquipmentRepository;
import com.sonumax2.javabot.util.NameNormUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipmentService {

    private final EquipmentRepository repo;

    public EquipmentService(EquipmentRepository repo) {
        this.repo = repo;
    }

    public List<Equipment> top(long chatId, int limit) {
        return repo.listActiveTop(chatId, limit);
    }

    public List<Equipment> search(long chatId, String raw, int limit) {
        String norm = NameNormUtils.normalizeNorm(raw);
        if (norm.isBlank()) return List.of();
        return repo.searchActive(chatId, norm, limit);
    }

    public Equipment create(long chatId, String rawName) {
        String name = rawName.trim();
        String nameNorm = NameNormUtils.normalizeNorm(name);

        return repo.findByChatIdAndNameNorm(chatId, nameNorm)
                .orElseGet(() -> {
                    Equipment e = new Equipment();
                    e.chatId = chatId;
                    e.name = name;
                    e.nameNorm = nameNorm;
                    e.isActive = true;
                    e.createdAt = LocalDateTime.now();
                    e.updatedAt = LocalDateTime.now();
                    return repo.save(e);
                });
    }
}
