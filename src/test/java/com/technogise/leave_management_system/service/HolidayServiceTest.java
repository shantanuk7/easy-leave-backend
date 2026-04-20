package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.HolidayRequest;
import com.technogise.leave_management_system.dto.HolidayResponse;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.enums.HolidayType;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {
    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayService holidayService;

    private Holiday mockHoliday;
    private HolidayRequest holidayRequest;

    @BeforeEach
    void setUp() {
        mockHoliday = Holiday.builder()
                .id(UUID.randomUUID())
                .name("Diwali")
                .type(HolidayType.FIXED)
                .date(LocalDate.of(2026, 11, 9))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(null)
                .build();

        holidayRequest = HolidayRequest.builder()
                .name("Diwali")
                .type(HolidayType.FIXED)
                .date(LocalDate.of(2026, 11, 9))
                .build();
    }

    @Test
    void shouldCreateAndReturnHolidaySuccessfully() {
        // When
        when(holidayRepository.save(any(Holiday.class))).thenReturn(mockHoliday);
        HolidayResponse response = holidayService.createHoliday(holidayRequest);

        // Then
        assertEquals(mockHoliday.getId(), response.getId());
        assertEquals(mockHoliday.getName(), response.getName());
        assertEquals(mockHoliday.getType(), response.getType());
        assertEquals(mockHoliday.getDate(), response.getDate());
    }

    @Test
    void shouldThrowExceptionWhenHolidayAlreadyExists() {
        // When
        when(holidayRepository.existsByNameIgnoreCaseAndDateBetween(
                any(String.class),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(true);

        // Then
        HttpException exception = assertThrows(HttpException.class,
                () -> holidayService.createHoliday(holidayRequest));
        assertEquals("Holiday already exists in the given year", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenHolidayDateAlreadyExists() {
        // When
        when(holidayRepository.existsByDate(any(LocalDate.class))).thenReturn(true);

        // Then
        HttpException exception = assertThrows(HttpException.class,
                () -> holidayService.createHoliday(holidayRequest));
        assertEquals("Holiday already exists on the given date", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenHolidayDateIsWeekend() {
        // Given
        HolidayRequest request = HolidayRequest.builder()
                .name("Independence Day")
                .type(HolidayType.FIXED)
                .date(LocalDate.of(2026, 8, 15))
                .build();

        // Then
        HttpException exception = assertThrows(HttpException.class, () -> holidayService.createHoliday(request));
        assertEquals("Holiday cannot be created on a weekend day", exception.getMessage());
    }

    @Test
    void shouldFetchAllHolidaysSuccessfully() {
        // When
        when(holidayRepository.findAll()).thenReturn(List.of(mockHoliday));
        List<HolidayResponse> responses = holidayService.getHolidays();

        // Then
        assertEquals(1, responses.size());
        assertEquals(mockHoliday.getId(), responses.getFirst().getId());
    }
}
