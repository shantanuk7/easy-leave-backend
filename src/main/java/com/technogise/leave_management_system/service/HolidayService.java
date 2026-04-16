package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.HolidayRequest;
import com.technogise.leave_management_system.dto.HolidayResponse;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.repository.HolidayRepository;
import org.springframework.stereotype.Service;

@Service
public class HolidayService {
    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public HolidayResponse createHoliday(HolidayRequest request) {
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
