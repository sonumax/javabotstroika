package com.sonumax2.javabot.model.reference;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.time.Instant;

public abstract class BaseRefEntity {

    @Id @Column("id") private Long id;
    @Column("name") private String name;
    @Column("is_active") private boolean active = true;
    @Column("created_by_chat_id") private Long createdByChatId;
    @Column("created_at") private Instant createdAt;
    @Column("name_norm") private String nameNorm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getCreatedByChatId() { return createdByChatId; }
    public void setCreatedByChatId(Long createdByChatId) { this.createdByChatId = createdByChatId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt;}

    public String getNameNorm() { return nameNorm; }
    public void setNameNorm(String nameNorm) { this.nameNorm = nameNorm; }
}