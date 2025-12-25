package com.jlgs.howmuchah.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "description", nullable = false)
    private String description;

    @Positive
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "SGD";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_user_id", nullable = false)
    private User paidBy;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_settled", nullable = false)
    @Builder.Default
    private boolean isSettled = false;

    /**
     * These two overrides are needed because we use Map<Expense, List<ExpenseSplit>>
     * during settlement calculation.
     *
     * Without them, Hibernate may create multiple Expense proxy objects in memory for
     * the same database row. Java's default equals() compares by memory address, so
     * these different objects would be treated as different map keys.
     *
     * This override tells Java to compare Expense objects by their ID (business identity)
     * instead of memory address, ensuring all splits for the same expense are grouped
     * together correctly.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expense)) return false;
        Expense expense = (Expense) o;
        return id != null && id.equals(expense.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}