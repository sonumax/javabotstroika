package com.sonumax2.javabot.domain.reference;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("equipment")
public class Equipment {
    @Id @Column("id") private Long id;

    @Column("chat_id") private Long chatId;
    @Column("name") private String name;
    @Column("name_norm") private String nameNorm;
    @Column("is_active") private boolean isActive = true;
    @Column("created_at") private LocalDateTime createdAt;
    @Column("updated_at") private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameNorm() { return nameNorm; }
    public void setNameNorm(String nameNorm) { this.nameNorm = nameNorm; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}