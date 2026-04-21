package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.constants.LeaveConstants;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.UUID;

@Service
public class AnnualLeaveService {

    private final AnnualLeaveRepository annualLeaveRepository;
    private final LeaveCategoryService leaveCategoryService;
    private static final double FULL_DAY_VALUE = 1.0;
    private static final double HALF_DAY_VALUE = 0.5;

    public AnnualLeaveService(AnnualLeaveRepository annualLeaveRepository,  LeaveCategoryService leaveCategoryService) {
        this.annualLeaveRepository = annualLeaveRepository;
        this.leaveCategoryService = leaveCategoryService;
    }

    public void createAnnualLeaveForNewEmployee(User user) {
        int currentYear = Year.now().getValue();
        String currentYearStr = String.valueOf(currentYear);

        if (annualLeaveRepository.findByUserIdAndYear(user.getId(), currentYearStr).isPresent()) {
            return;
        }

        int joiningMonth = user.getCreatedAt().getMonthValue();
        int monthsRemaining = 13 - joiningMonth;
        double totalAllocated = leaveCategoryService.getAllocatedDaysByCategoryName(LeaveConstants.ANNUAL_LEAVE);
        double proratedTotal = (totalAllocated / 12.0) * monthsRemaining;

        AnnualLeave annualLeave = new AnnualLeave();
        annualLeave.setUser(user);
        annualLeave.setTotal(proratedTotal);
        annualLeave.setTaken(0.0);
        annualLeave.setYear(currentYearStr);
        annualLeave.setBalance(proratedTotal);
        annualLeaveRepository.save(annualLeave);
    }

    public void syncOnLeaveCreated(User user, DurationType duration, int numberOfDates, int year) {
        AnnualLeave annualLeave = getAnnualLeave(user.getId(), year);

        double leaveDaysChange = (duration == DurationType.FULL_DAY ? FULL_DAY_VALUE : HALF_DAY_VALUE) * numberOfDates;

        annualLeave.setTaken(annualLeave.getTaken() + leaveDaysChange);
        annualLeave.setBalance(annualLeave.getBalance() - leaveDaysChange);

        annualLeaveRepository.save(annualLeave);
    }

    public void syncOnLeaveUpdated(User user, String oldCategoryName, String newCategoryName, DurationType oldDuration,
                                   DurationType newDuration, int year) {

        boolean wasAnnual = oldCategoryName.equals(LeaveConstants.ANNUAL_LEAVE);
        boolean isAnnual = newCategoryName.equals(LeaveConstants.ANNUAL_LEAVE);

        if (wasAnnual && isAnnual) {
            if (oldDuration == newDuration) {
                return;
            }

            AnnualLeave annualLeave = getAnnualLeave(user.getId(), year);
            double leaveDaysChange = (newDuration == DurationType.FULL_DAY) ? HALF_DAY_VALUE : -HALF_DAY_VALUE;
            annualLeave.setTaken(annualLeave.getTaken() + leaveDaysChange);
            annualLeave.setBalance(annualLeave.getBalance() - leaveDaysChange);
            annualLeaveRepository.save(annualLeave);
        } else if (!wasAnnual && isAnnual) {
            syncOnLeaveCreated(user, newDuration, 1, year);
        }
    }

    private AnnualLeave getAnnualLeave(UUID userId, int year) {
        return annualLeaveRepository.findByUserIdAndYear(userId, String.valueOf(year))
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "Annual leave record not found for user: " + userId
                                + " and year: " + year));
    }
}
