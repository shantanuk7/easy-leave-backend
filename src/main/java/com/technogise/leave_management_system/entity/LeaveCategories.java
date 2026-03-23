package com.technogise.leave_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "leave_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveCategories {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name",unique = true, length = 50)
    private String name;

    @Column(name = "allocated_days", nullable = false)
    private String allocatedDays;

    @Column(name = "year", nullable = false, length = 25)
    private String year;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
