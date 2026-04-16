package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull
    private UUID employeeId;

    @NotNull
    private UserRole role;
}
