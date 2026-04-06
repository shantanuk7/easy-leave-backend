package com.technogise.leave_management_system.dto;
import com.technogise.leave_management_system.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class UserResponse {
    private UUID id;
    private String email;
    private String name;
    private UserRole role;
}

