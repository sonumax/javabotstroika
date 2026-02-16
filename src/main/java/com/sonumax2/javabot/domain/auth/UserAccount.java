package com.sonumax2.javabot.domain.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("user_account")
public class UserAccount {

    @Id
    @Column("chat_id")
    private Long chatId;

    @Column("role")
    private UserRole role = UserRole.USER;

    @Column("status")
    private UserStatus status = UserStatus.PENDING;

    @Column("first_name")
    private String firstName;

    @Column("username")
    private String username;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("requested_at")
    private LocalDateTime requestedAt;

    @Column("approved_at")
    private LocalDateTime approvedAt;

    @Column("approved_by")
    private Long approvedBy;

    @Column("note")
    private String note;

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
