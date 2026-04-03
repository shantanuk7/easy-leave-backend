package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.AnnualLeaveBalanceResponse;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import static java.util.stream.Collectors.toList;

@Service
public class AnnualLeaveBalanceService {
    private final AnnualLeaveRepository annualLeaveRepository;
    private final UserService userService;

    public AnnualLeaveBalanceService(AnnualLeaveRepository annualLeaveRepository, UserService userService) {
        this.annualLeaveRepository = annualLeaveRepository;
        this.userService = userService;
    }

    public List<AnnualLeaveBalanceResponse> getAnnualLeaveBalancesForAllEmployees(int year) {

        List<User> employees = userService.getAllEmployees();

        return employees.stream()
                .map(employee -> getAnnualLeaveBalanceForEmployee(employee, year))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    private Optional<AnnualLeaveBalanceResponse> getAnnualLeaveBalanceForEmployee(User employee, int year) {

        Optional<AnnualLeave> annualLeaveOpt = annualLeaveRepository
                .findByUserIdAndYear(employee.getId(), String.valueOf(year));

        if (annualLeaveOpt.isEmpty()) {
            return Optional.empty();
        }

        AnnualLeave annualLeave = annualLeaveOpt.get();

        return Optional.of(new AnnualLeaveBalanceResponse(employee.getId().toString(), employee.getName(),
                Double.parseDouble(annualLeave.getTotal()),
                Double.parseDouble(annualLeave.getTaken()),
                Double.parseDouble(annualLeave.getBalance())
        ));
    }
}
