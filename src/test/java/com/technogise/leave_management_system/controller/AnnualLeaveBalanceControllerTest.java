package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.AnnualLeaveBalanceResponse;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.AnnualLeaveBalanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnualLeaveBalanceControllerTest {

    @Mock
    private AnnualLeaveBalanceService annualLeaveBalanceService;

    @InjectMocks
    private AnnualLeaveBalanceController annualLeaveBalanceController;

    private static final int CURRENT_YEAR = Year.now().getValue();

    private AnnualLeaveBalanceResponse createLeaveBalanceResponse() {
        return new AnnualLeaveBalanceResponse(
                UUID.randomUUID().toString(), "Arjun", 24.0, 5.0, 19.0
        );
    }

    @Test
    void shouldReturnEmptyDataWithNoEmployeesFoundMessageWhenNoDataExist() {

        Pageable pageable = Pageable.unpaged();
        Page<AnnualLeaveBalanceResponse> emptyPage = Page.empty();

        when(annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR, pageable)).thenReturn(emptyPage);

        ResponseEntity<SuccessResponse<Page<AnnualLeaveBalanceResponse>>> response =
                annualLeaveBalanceController.getAnnualLeaveBalance(null, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assert response.getBody() != null;
        assertTrue(response.getBody().getData().isEmpty());
        assertEquals("No employees leave balance found", response.getBody().getMessage());
    }

    @Test
    void shouldReturnLeaveBalancesWhenDataExists() {

        Pageable pageable = Pageable.unpaged();

        Page<AnnualLeaveBalanceResponse> page = new PageImpl<>(List.of(createLeaveBalanceResponse()));

        when(annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR, pageable)).thenReturn(page);

        ResponseEntity<SuccessResponse<Page<AnnualLeaveBalanceResponse>>> response =
                annualLeaveBalanceController.getAnnualLeaveBalance(null, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assert response.getBody() != null;
        assertEquals(1, response.getBody().getData().getContent().size());
        assertEquals("Employee leave balance fetched successfully", response.getBody().getMessage());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void shouldUsePassedYearWhenYearParamIsProvided() {

        Pageable pageable = Pageable.unpaged();

        Page<AnnualLeaveBalanceResponse> page = new PageImpl<>(List.of(createLeaveBalanceResponse()));

        when(annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(2023, pageable)).thenReturn(page);

        ResponseEntity<SuccessResponse<Page<AnnualLeaveBalanceResponse>>> response =
                annualLeaveBalanceController.getAnnualLeaveBalance(2023, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assert response.getBody() != null;
        assertEquals(1, response.getBody().getData().getContent().size());
    }
}
