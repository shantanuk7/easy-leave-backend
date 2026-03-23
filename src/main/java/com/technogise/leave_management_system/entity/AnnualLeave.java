package com.technogise.leave_management_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "annual_leaves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnualLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total", nullable = false)
    private String total;

    @Column(name = "carry_forward_count", nullable = false)
    private String carryForwardCount = "0";

    @Column(name = "compensatory_off_count", nullable = false)
    private String compensatoryOffCount = "0";

    @Column(name = "taken", nullable = false)
    private String taken = "0";

    @Column(name = "balance", nullable = false)
    private String balance;

    @Column(name = "leave_year", nullable = false, length = 25)
    private String year;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.balance == null) this.balance = this.total;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}