package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.RequestResponse;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.Request;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import com.technogise.leave_management_system.enums.ScopeType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RequestService requestService;

    private User employee;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Priyansh");
        employee.setRole(UserRole.EMPLOYEE);

        pageable = PageRequest.of(0, 10);
    }

    private Request buildRequest(User user, LeaveCategory leaveCategory) {
        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setRequestedByUser(user);
        request.setRequestType(RequestType.PAST_LEAVE);
        request.setLeaveCategory(leaveCategory);
        request.setDate(LocalDate.now());
        request.setDuration(DurationType.FULL_DAY);
        request.setDescription("Sick");
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    @Test
    void shouldReturnRequestsForSelfScopeWithoutStatusFilter() {
        Request request = buildRequest(employee, null);
        Page<Request> mockPage = new PageImpl<>(List.of(request));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(requestRepository.findAllByRequestedByUserId(eq(employee.getId()), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<RequestResponse> result = requestService.getAllRequests(pageable, employee.getId(), ScopeType.SELF, null);
        assertEquals(1, result.getContent().size());
        assertEquals("Priyansh", result.getContent().getFirst().getEmployeeName());
        assertEquals("Sick", result.getContent().getFirst().getDescription());
    }

    @Test
    void shouldReturnRequestsForSelfScopeWithStatusFilter() {
        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setName("Sick Leave");

        Request request = buildRequest(employee, leaveCategory);
        Page<Request> mockPage = new PageImpl<>(List.of(request));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(requestRepository.findAllByRequestedByUserIdAndStatus(
                eq(employee.getId()), eq(RequestStatus.PENDING), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<RequestResponse> result = requestService.getAllRequests(
                pageable, employee.getId(), ScopeType.SELF, RequestStatus.PENDING);

        assertEquals(1, result.getContent().size());
        assertEquals("Priyansh", result.getContent().getFirst().getEmployeeName());
        assertEquals("Sick Leave", result.getContent().getFirst().getLeaveCategory());
    }

    @Test
    void shouldThrowBadRequestForInvalidScope() {
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);

        HttpException exception = assertThrows(HttpException.class, () ->
                requestService.getAllRequests(
                        pageable, employee.getId(), null, null));

        assertTrue(exception.getMessage().contains("Invalid scope"));
    }
}
