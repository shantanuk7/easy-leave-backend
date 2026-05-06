package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UpdateRequestPayload {
    @NotNull(message = "Request type must not be null")
    private RequestType requestType;

    @NotNull(message = "Request status must not be null")
    private RequestStatus status;

    @Size(max = 1000, message = "Remark must not exceed 1000 characters")
    private String managerRemark;
}
