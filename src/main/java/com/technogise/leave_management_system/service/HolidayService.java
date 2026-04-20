package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.HolidayRequest;
import com.technogise.leave_management_system.dto.HolidayResponse;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.enums.WeekendDay;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.HolidayRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class HolidayService {
    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    private void validateDuplicateHolidayInYear(String name, LocalDate date) {
        int year = date.getYear();

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        boolean exists = holidayRepository
                .existsByNameIgnoreCaseAndDateBetween(name, startDate, endDate);

        if (exists) {
            throw new HttpException(HttpStatus.CONFLICT,
                    "Holiday already exists in the given year");
        }
    }

    private void validateDuplicateDate(LocalDate date) {
        if (holidayRepository.existsByDate(date)) {
            throw new HttpException(HttpStatus.CONFLICT,
                    "Holiday already exists on the given date");
        }
    }

    public void validateWeekendDay(LocalDate date) {
        boolean isWeekend = Arrays.stream(WeekendDay.values())
                .anyMatch(weekend -> weekend.getDayOfWeek() == date.getDayOfWeek());

        if (isWeekend) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Holiday cannot be created on a weekend day");
        }
    }

    public HolidayResponse createHoliday(HolidayRequest request) {
        validateDuplicateHolidayInYear(request.getName(), request.getDate());
        validateDuplicateDate(request.getDate());
        validateWeekendDay(request.getDate());

        Holiday holiday = Holiday.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .date(request.getDate())
                .build();

        Holiday savedHoliday = holidayRepository.save(holiday);

        return new HolidayResponse(
                savedHoliday.getId(),
                savedHoliday.getName(),
                savedHoliday.getType(),
                savedHoliday.getDate()
        );
    }

    public List<HolidayResponse> getHolidays() {
        List<Holiday> holidays = holidayRepository.findAll();

        return holidays.stream()
                .map(holiday -> new HolidayResponse(
                        holiday.getId(),
                        holiday.getName(),
                        holiday.getType(),
                        holiday.getDate()
                ))
                .toList();
    }
}
