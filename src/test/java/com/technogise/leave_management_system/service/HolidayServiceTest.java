package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.HolidayRequest;
import com.technogise.leave_management_system.dto.HolidayResponse;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.enums.HolidayType;
import com.technogise.leave_management_system.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {
    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayService holidayService;

    private Holiday savedHoliday;
    private HolidayRequest holidayRequest;

    @BeforeEach
    void setUp() {
        savedHoliday = Holiday.builder()
                .id(UUID.randomUUID())
                .name("Diwali")
                .type(HolidayType.FIXED)
                .date(LocalDate.of(2026, 11, 8))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(null)
                .build();

        holidayRequest = HolidayRequest.builder()
                .name("Diwali")
                .type(HolidayType.FIXED)
                .date(LocalDate.of(2026, 11, 8))
                .build();
    }

    @Test
    void shouldCreateAndReturnHolidaySuccessfully() {
        // When
        when(holidayRepository.save(any(Holiday.class))).thenReturn(savedHoliday);
        HolidayResponse response = holidayService.createHoliday(holidayRequest);

        // Then
        assertEquals(savedHoliday.getId(), response.getId());
        assertEquals(savedHoliday.getName(), response.getName());
        assertEquals(savedHoliday.getType(), response.getType());
        assertEquals(savedHoliday.getDate(), response.getDate());
    }
}
