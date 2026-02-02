package com.sonumax2.javabot.domain.reference;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("equipment")
public class Equipment {

    @Id
    public Long id;

    public Long chatId;

    public String name;
    public String nameNorm;

    public boolean isActive = true;

    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
