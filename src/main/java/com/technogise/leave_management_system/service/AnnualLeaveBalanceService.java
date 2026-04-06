package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.AnnualLeaveBalanceResponse;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnualLeaveBalanceService {
    private final AnnualLeaveRepository annualLeaveRepository;

    public AnnualLeaveBalanceService(AnnualLeaveRepository annualLeaveRepository) {
        this.annualLeaveRepository = annualLeaveRepository;
    }

    public Page<AnnualLeaveBalanceResponse> getAnnualLeaveBalancesForAllEmployees(int year, Pageable pageable) {

        return annualLeaveRepository.findAllByYear(String.valueOf(year), pageable)
                .map(annualLeave -> new AnnualLeaveBalanceResponse(annualLeave.getUser().getId().toString(),
                        annualLeave.getUser().getName(),
                        Double.parseDouble(annualLeave.getTotal()),
                        Double.parseDouble(annualLeave.getTaken()),
                        Double.parseDouble(annualLeave.getBalance())
                ));
    }

    public List<String> getDistinctYears() {
        return annualLeaveRepository.findDistinctYears();
    }
}
