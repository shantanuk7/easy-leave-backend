package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.LeaveCategoryResponse;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.repository.LeaveCategoryRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.JwtService;
import com.technogise.leave_management_system.service.LeaveCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = LeaveCategoryController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
public class LeaveCategoryControllerTest {

    @MockitoBean
    private LeaveCategoryService leaveCategoryService;

    @MockitoBean
    private LeaveCategoryRepository leaveCategoryRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private MockMvc mockMvc;

    LeaveCategory leaveCategory = new LeaveCategory();
    User user = new User();

    @BeforeEach
    void setup() {
        user.setId(UUID.randomUUID());
        user.setName("Shantanu");
        user.setEmail("shantanu@technogise.com");
        user.setRole(UserRole.EMPLOYEE);

        leaveCategory.setId(UUID.randomUUID());
        leaveCategory.setName("Annual Leave");
    }

    private RequestPostProcessor mockUser(User user) {
        return request -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @Test
    void shouldRespondWithAllLeaveCategoriesAnd200Status() throws Exception {
        List<LeaveCategoryResponse> response = List.of(
                new LeaveCategoryResponse(
                        leaveCategory.getId(),
                        leaveCategory.getName()
                )
        );

        when(leaveCategoryService.getAllLeaveCategories()).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leave-categories")
                        .with(mockUser(user)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data[0].id").value(leaveCategory.getId().toString()))
                .andExpect(jsonPath("$.data[0].name").value(leaveCategory.getName()));
    }

    @Test
    void shouldReturnEmptyListAnd200StatusWhenNoLeaveCategoriesExist() throws Exception {
        List<LeaveCategoryResponse> response = List.of();

        when(leaveCategoryService.getAllLeaveCategories()).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leave-categories")
                        .with(mockUser(user)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
