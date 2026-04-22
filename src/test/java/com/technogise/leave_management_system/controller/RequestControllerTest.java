package com.technogise.leave_management_system.controller;
import com.technogise.leave_management_system.dto.GetAllRequestResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.ScopeType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.JwtService;
import com.technogise.leave_management_system.service.RequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = RequestController.class, excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class,
        OAuth2ClientWebSecurityAutoConfiguration.class
})
class RequestControllerTest {

    @MockitoBean
    private RequestService requestService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private User employee;

    private RequestPostProcessor mockUser(User user) {
        return request -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Priyansh Saxena");
        employee.setEmail("priyansh@technogise.com");
        employee.setRole(UserRole.EMPLOYEE);
    }

    @Test
    void shouldReturn200AndListOfRequestsForSelfScope() throws Exception {
        List<GetAllRequestResponse> mockResponse = List.of( new GetAllRequestResponse(
                UUID.randomUUID(),
                employee.getName(),
                null,
                "Sick Leave",
                LocalDate.now(),
                DurationType.FULL_DAY,
                "Fever",
                RequestStatus.PENDING,
                LocalDate.now()
        ));
        Page<GetAllRequestResponse> mockPage = new PageImpl<>(mockResponse);
        when(requestService.getAllRequests( any(Pageable.class), eq(employee.getId()), eq(ScopeType.SELF), eq(null))).thenReturn(mockPage);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/requests")
                        .param("scope", "SELF")
                        .param("page", "0")
                        .param("size", "20")
                        .with(mockUser(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Requests fetch Successfully"))
                .andExpect(jsonPath("$.data.content[0].employeeName").value("Priyansh Saxena"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }
    @Test
    void shouldReturn403WhenEmployeeAccessesOrganizationScope() throws Exception {
        when(requestService.getAllRequests(
                any(Pageable.class),
                eq(employee.getId()),
                eq(ScopeType.ORGANIZATION),
                any()
        )).thenThrow(new HttpException(HttpStatus.FORBIDDEN, "Not allowed to access this resource"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/requests")
                        .param("scope", "ORGANIZATION")
                        .with(mockUser(employee)))
                .andExpect(status().isForbidden());
    }
}
