package com.technogise.leave_management_system.entity;

import com.technogise.leave_management_system.exception.HttpException;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "annual_leaves")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AnnualLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total", nullable = false)
    private String total;

    @Column(name = "taken", nullable = false)
    private String taken = "0";

    @Column(name = "balance", nullable = false)
    private String balance;

    @Column(name = "leave_year", nullable = false)
    private String year;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        validateTotal();
        if (this.balance == null) {
            this.balance = this.total;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    private void validateTotal() {
        if (this.total == null || this.total.isBlank()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Total Annual leave cannot be null or empty");
        }
        try {
            double value = Double.parseDouble(this.total);
            if (value < 0) {
                throw new HttpException(HttpStatus.BAD_REQUEST, "Annual leave total cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Annual leave total must be a valid number");
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
