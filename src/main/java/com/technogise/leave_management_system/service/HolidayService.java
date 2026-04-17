package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.HolidayRequest;
import com.technogise.leave_management_system.dto.HolidayResponse;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.enums.WeekendDay;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.HolidayRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;

@Service
public class HolidayService {
    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    private void validateDuplicateHolidayInYear(String name) {
        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 1, 1);
        LocalDate endDate = LocalDate.of(currentYear, 12, 31);

        Holiday holidayExists = holidayRepository
                .findByNameAndDateBetween(name, startDate, endDate);

        if (holidayExists != null) {
            throw new HttpException(HttpStatus.CONFLICT,
                    "Holiday already exists in the current year");
        }
    }

    private void validateDuplicateDate(LocalDate date) {
        Holiday dateExists = holidayRepository.findByDate(date);

        if (dateExists != null) {
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
        validateDuplicateHolidayInYear(request.getName());
        validateDuplicateDate(request.getDate());
        validateWeekendDay(request.getDate());

        Holiday holiday = Holiday.builder()
                .name(request.getName())
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
}
