package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.HolidayRequest;
import com.technogise.leave_management_system.dto.HolidayResponse;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.HolidayService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {
    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<HolidayResponse>> createHoliday(@RequestBody @Valid HolidayRequest request) {
        log.info("POST /api/holidays");
        HolidayResponse response = holidayService.createHoliday(request);

        log.debug("Holiday created successfully");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success("Holiday created successfully", response));
    }
}
