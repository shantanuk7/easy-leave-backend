package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UpdateRequestPayload {
    private RequestType requestType;

    private RequestStatus status;

    private String managerRemark;
}
