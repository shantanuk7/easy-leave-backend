package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.constants.LeaveConstants;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnualLeaveServiceTest {

    @Mock
    private AnnualLeaveRepository annualLeaveRepository;

    @Mock
    private LeaveCategoryService leaveCategoryService;

    @InjectMocks
    private AnnualLeaveService annualLeaveService;

    private static final int CURRENT_YEAR = Year.now().getValue();
    private static final String CURRENT_YEAR_STR = String.valueOf(CURRENT_YEAR);
    private static final double ANNUAL_LEAVE_ALLOCATED_DAYS = 24.0;

    private User createUser(int joiningMonth) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Rakshit");
        user.setCreatedAt(LocalDateTime.of(CURRENT_YEAR, joiningMonth, 1, 0, 0));
        return user;
    }

    private AnnualLeave createAnnualLeave(User user, double total, double taken, double balance) {
        AnnualLeave annualLeave = new AnnualLeave();
        annualLeave.setId(UUID.randomUUID());
        annualLeave.setUser(user);
        annualLeave.setTotal(total);
        annualLeave.setTaken(taken);
        annualLeave.setBalance(balance);
        annualLeave.setYear(CURRENT_YEAR_STR);
        return annualLeave;
    }

    private void mockCategoryAllocatedDays() {
        when(leaveCategoryService.getAllocatedDaysByCategoryName(LeaveConstants.ANNUAL_LEAVE)).thenReturn((int) ANNUAL_LEAVE_ALLOCATED_DAYS);
    }

    @Test
    void shouldCreateAnnualLeaveWithFullAllocationWhenEmployeeJoinsInJanuary() {
        User user = createUser(1);
        mockCategoryAllocatedDays();
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR)).thenReturn(Optional.empty());

        annualLeaveService.createAnnualLeaveForNewEmployee(user);

        verify(annualLeaveRepository).save(argThat(al -> al.getTotal() == 24.0 && al.getTaken() == 0.0
                && al.getBalance() == 24.0 && al.getYear().equals(CURRENT_YEAR_STR)));
    }

    @Test
    void shouldCreateAnnualLeaveWithProratedAllocationWhenEmployeeJoinsInJuly() {
        User user = createUser(7);
        mockCategoryAllocatedDays();
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR)).thenReturn(Optional.empty());

        annualLeaveService.createAnnualLeaveForNewEmployee(user);

        verify(annualLeaveRepository).save(argThat(al -> al.getTotal() == 12.0 && al.getTaken() == 0.0
                && al.getBalance() == 12.0 && al.getYear().equals(CURRENT_YEAR_STR)
        ));
    }

    @Test
    void shouldCreateAnnualLeaveWithMinimumAllocationWhenEmployeeJoinsInDecember() {
        User user = createUser(12);
        mockCategoryAllocatedDays();
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR)).thenReturn(Optional.empty());

        annualLeaveService.createAnnualLeaveForNewEmployee(user);

        verify(annualLeaveRepository).save(argThat(al -> al.getTotal() == 2.0 && al.getTaken() == 0.0
                && al.getBalance() == 2.0 && al.getYear().equals(CURRENT_YEAR_STR)
        ));
    }

    @Test
    void shouldSkipCreationWhenAnnualLeaveRowAlreadyExistsForCurrentYear() {
        User user = createUser(1);
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR))
                .thenReturn(Optional.of(createAnnualLeave(user, 24.0, 0.0, 24.0)));

        annualLeaveService.createAnnualLeaveForNewEmployee(user);

        verify(annualLeaveRepository, never()).save(any());
        verify(leaveCategoryService, never()).getAllocatedDaysByCategoryName(any());
    }

    @Test
    void shouldIncreaseTakenAndDecreaseBalanceWhenFullDayLeaveCreated() {
        User user = createUser(1);
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR))
                .thenReturn(Optional.of(createAnnualLeave(user, 24.0, 0.0, 24.0)));

        annualLeaveService.syncOnLeaveCreated(user, DurationType.FULL_DAY, 1, CURRENT_YEAR);

        verify(annualLeaveRepository).save(argThat(al -> al.getTaken() == 1.0 && al.getBalance() == 23.0
        ));
    }

    @Test
    void shouldIncreaseTakenByHalfWhenHalfDayLeaveCreated() {
        User user = createUser(1);
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR))
                .thenReturn(Optional.of(createAnnualLeave(user, 24.0, 0.0, 24.0)));

        annualLeaveService.syncOnLeaveCreated(user, DurationType.HALF_DAY, 1, CURRENT_YEAR);

        verify(annualLeaveRepository).save(argThat(al -> al.getTaken() == 0.5 && al.getBalance() == 23.5));
    }

    @Test
    void shouldIncreaseTakenByMultipleDaysWhenMultipleLeavesCreated() {
        User user = createUser(1);
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR))
                .thenReturn(Optional.of(createAnnualLeave(user, 24.0, 0.0, 24.0)));

        annualLeaveService.syncOnLeaveCreated(user, DurationType.FULL_DAY, 5, CURRENT_YEAR);

        verify(annualLeaveRepository).save(argThat(al -> al.getTaken() == 5.0 && al.getBalance() == 19.0));
    }

    @Test
    void shouldThrowNotFoundWhenAnnualLeaveRecordMissingOnLeaveCreated() {
        User user = createUser(1);
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR)).thenReturn(Optional.empty());

        assertThrows(HttpException.class, () -> annualLeaveService.syncOnLeaveCreated(user, DurationType.FULL_DAY, 1, CURRENT_YEAR));
    }

    @Test
    void shouldDecreaseTakenWhenLeaveUpdatedFromFullDayToHalfDay() {
        User user = createUser(1);
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR))
                .thenReturn(Optional.of(createAnnualLeave(user, 24.0, 3.0, 21.0)));

        annualLeaveService.syncOnLeaveUpdated(user, LeaveConstants.ANNUAL_LEAVE, LeaveConstants.ANNUAL_LEAVE,
                DurationType.FULL_DAY, DurationType.HALF_DAY, CURRENT_YEAR);

        verify(annualLeaveRepository).save(argThat(al -> al.getTaken() == 2.5 && al.getBalance() == 21.5));
    }

    @Test
    void shouldIncreaseTakenWhenLeaveUpdatedFromHalfDayToFullDay() {
        User user = createUser(1);
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR))
                .thenReturn(Optional.of(createAnnualLeave(user, 24.0, 0.5, 23.5)));

        annualLeaveService.syncOnLeaveUpdated(user, LeaveConstants.ANNUAL_LEAVE, LeaveConstants.ANNUAL_LEAVE,
                DurationType.HALF_DAY, DurationType.FULL_DAY, CURRENT_YEAR);

        verify(annualLeaveRepository).save(argThat(al -> al.getTaken() == 1.0 && al.getBalance() == 23.0));
    }

    @Test
    void shouldNotSyncWhenDurationNotChangedOnLeaveUpdate() {
        User user = createUser(1);

        annualLeaveService.syncOnLeaveUpdated(
                user,LeaveConstants.ANNUAL_LEAVE,LeaveConstants.ANNUAL_LEAVE, DurationType.FULL_DAY, DurationType.FULL_DAY, CURRENT_YEAR);

        verify(annualLeaveRepository, never()).findByUserIdAndYear(any(), any());
        verify(annualLeaveRepository, never()).save(any());
    }

    @Test
    void shouldNotSyncWhenBothCategoriesAreNonAnnual() {
        User user = createUser(1);

        annualLeaveService.syncOnLeaveUpdated(user, "Sick Leave", "Sick Leave",
                DurationType.FULL_DAY, DurationType.HALF_DAY, CURRENT_YEAR);

        verify(annualLeaveRepository, never()).findByUserIdAndYear(any(), any());
        verify(annualLeaveRepository, never()).save(any());
    }

    @Test
    void shouldIncreaseTakenWhenCategoryChangedFromNonAnnualToAnnual() {
        User user = createUser(1);
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR))
                .thenReturn(Optional.of(createAnnualLeave(user, 24.0, 0.0, 24.0)));

        annualLeaveService.syncOnLeaveUpdated(user, "Sick Leave", LeaveConstants.ANNUAL_LEAVE,
                DurationType.FULL_DAY, DurationType.FULL_DAY, CURRENT_YEAR);

        verify(annualLeaveRepository).save(argThat(al -> al.getTaken() == 1.0 && al.getBalance() == 23.0));
    }

    @Test
    void shouldThrowNotFoundWhenAnnualLeaveRecordMissingOnLeaveUpdated() {
        User user = createUser(1);
        when(annualLeaveRepository.findByUserIdAndYear(user.getId(), CURRENT_YEAR_STR)).thenReturn(Optional.empty());

        assertThrows(HttpException.class, () -> annualLeaveService.syncOnLeaveUpdated(user, LeaveConstants.ANNUAL_LEAVE, LeaveConstants.ANNUAL_LEAVE,
                        DurationType.FULL_DAY, DurationType.HALF_DAY, CURRENT_YEAR));
    }

    @Test
    void shouldNotSyncWhenCategoryChangesFromAnnualLeaveToAnyOtherLeaveCategory() {
        User user = createUser(1);

        annualLeaveService.syncOnLeaveUpdated(user, LeaveConstants.ANNUAL_LEAVE, "Sick Leave", DurationType.FULL_DAY,
                DurationType.FULL_DAY, CURRENT_YEAR);
        verify(annualLeaveRepository, never()).findByUserIdAndYear(any(), any());
        verify(annualLeaveRepository, never()).save(any());
    }

}
