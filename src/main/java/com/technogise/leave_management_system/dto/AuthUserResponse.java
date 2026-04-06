package com.technogise.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AuthUserResponse {
    private UUID id;
    private String name;
    private String email;
    private String role;
}
