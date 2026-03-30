package com.technogise.leave_management_system.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SuccessResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> SuccessResponse<T> success(String message, T data) {
        return new SuccessResponse<>(true, message, data);
    }
}
