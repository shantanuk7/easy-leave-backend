package com.technogise.leave_management_system.entity;

import com.technogise.leave_management_system.enums.PlateformType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.*;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "leave_integration_events")
public class LeaveIntegrationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_id", nullable = false)
    private Leave leave;

    @Column(name = "platform", nullable = false)
    @Enumerated(EnumType.STRING)
    private PlateformType platform;

    @Column(name = "external_event_id", nullable = false)
    private String externalEventId;
}
