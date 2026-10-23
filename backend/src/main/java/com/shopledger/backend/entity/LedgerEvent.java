package com.shopledger.backend.entity;

import com.shopledger.backend.enums.LedgerEventType;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "ledger_event",
    indexes = {
        @Index(
            name = "idx_ledger_event_party_id",
            columnList = "party_id, occurred_at"
        ),
        @Index(
            name = "idx_ledger_event_item_id",
            columnList = "item_id, occurred_at"
        )
    }
)
public class LedgerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "event_type", nullable = false, columnDefinition = "ledger_event_type")
    private LedgerEventType eventType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "notes")
    private String notes;

    protected LedgerEvent() {
    }

    public UUID getId() {
        return id;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public LedgerEventType getEventType() {
        return eventType;
    }

    public void setEventType(LedgerEventType eventType) {
        this.eventType = eventType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = OffsetDateTime.now();
        }
    }
    public static LedgerEvent createPurchase(
        Party party,
        Item item,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        UUID idempotencyKey,
        String createdBy,
        String notes) {

            LedgerEvent event = new LedgerEvent();
            event.setParty(party);
            event.setItem(item);
            event.setEventType(LedgerEventType.PURCHASE_RECEIVED);
            event.setQuantity(quantity);
            event.setUnitPrice(unitPrice);
            event.setAmount(amount);
            event.setIdempotencyKey(idempotencyKey);
            event.setCreatedBy(createdBy);
            event.setNotes(notes);
            return event;
        }
        public static LedgerEvent createSale(
            Party party,
            Item item,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            UUID idempotencyKey,
            String createdBy,
            String notes) {

        LedgerEvent event = new LedgerEvent();
        event.setParty(party);
        event.setItem(item);
        event.setEventType(LedgerEventType.SALE);
        event.setQuantity(quantity);
        event.setUnitPrice(unitPrice);
        event.setAmount(amount);
        event.setIdempotencyKey(idempotencyKey);
        event.setCreatedBy(createdBy);
        event.setNotes(notes);

        return event;
    }

    public static LedgerEvent createPayment(
            Party party,
            LedgerEventType eventType,
            BigDecimal amount,
            UUID idempotencyKey,
            String createdBy,
            String notes) {

        if (eventType != LedgerEventType.PAYMENT_MADE
                && eventType != LedgerEventType.PAYMENT_RECEIVED) {
            throw new IllegalArgumentException(
                    "Payment event type must be PAYMENT_MADE or PAYMENT_RECEIVED.");
        }

        if (amount == null || amount.signum() >= 0) {
            throw new IllegalArgumentException(
                    "Payment ledger amount must be negative.");
        }

        LedgerEvent event = new LedgerEvent();
        event.setParty(party);
        event.setItem(null);
        event.setEventType(eventType);
        event.setQuantity(null);
        event.setUnitPrice(null);
        event.setAmount(amount);
        event.setIdempotencyKey(idempotencyKey);
        event.setCreatedBy(createdBy);
        event.setNotes(notes);

        return event;
    }

}
