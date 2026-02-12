package com.sonumax2.javabot.domain.operation;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Table("operation")
public class Operation {
    @Id
    @Column("id") private Long id;
    @Column("chat_id") private Long chatId;
    @Column("op_type") private OperationType opType;
    @Column("op_date") private LocalDate opDate;
    @Column("amount") private BigDecimal amount;
    @Column("note") private String note;
    @Column("created_at") private LocalDateTime createdAt;
    @Column("updated_at") private LocalDateTime updatedAt;
    @Column("updated_by") private Long updatedBy;
    @Column("is_cancelled") private boolean cancelled  = false;
    @Column("cancelled_at") private LocalDateTime cancelledAt;
    @Column("cancelled_by") private Long cancelledBy;
    @Column("cancel_reason") private String cancelReason;
    @Column("exported_at") private LocalDateTime exportedAt;
    @Column("photo_file_id") private String photoFileId;

    @MappedCollection(idColumn = "operation_id")
    private Set<FuelDetail> fuelDetails = new HashSet<>();

    @MappedCollection(idColumn = "operation_id")
    private Set<SypDetail> sypDetails = new HashSet<>();

    @MappedCollection(idColumn="operation_id")
    private Set<SypItem> sypItems = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public OperationType getOpType() { return opType; }
    public void setOpType(OperationType opType) { this.opType = opType; }

    public LocalDate getOpDate() { return opDate; }
    public void setOpDate(LocalDate opDate) { this.opDate = opDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public boolean isCancelled() { return cancelled ; }
    public void setCancelled(boolean cancelled) {  this.cancelled = cancelled; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Long getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(Long cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public LocalDateTime getExportedAt() { return exportedAt; }
    public void setExportedAt(LocalDateTime exportedAt) { this.exportedAt = exportedAt; }

    public String getPhotoFileId() { return photoFileId; }
    public void setPhotoFileId(String photoFileId) { this.photoFileId = photoFileId; }

    public void setFuelDetail(FuelDetail d) {
        fuelDetails.clear();
        if (d != null) fuelDetails.add(d);
        // enforce single detail type per operation
        sypDetails.clear();
    }

    public void setSypDetail(SypDetail d) {
        sypDetails.clear();
        if (d != null) sypDetails.add(d);
        // enforce single detail type per operation
        fuelDetails.clear();
    }

    public void replaceSypItems(Collection<SypItem> items) {
        this.sypItems.clear();
        if (items != null) {
            this.sypItems.addAll(items);
        }
    }

    public FuelDetail getFuelDetailOrNull() {
        return fuelDetails.isEmpty() ? null : fuelDetails.iterator().next();
    }

    public SypDetail getSypDetailOrNull() {
        return sypDetails.isEmpty() ? null : sypDetails.iterator().next();
    }

    public Set<SypItem> getSypItems() {
        return sypItems;
    }

}