package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {
    @InjectMocks
    private JwtService jwtService;

    private User testUser;

    private static final String TEST_SECRET =
            "8tI5heWRJh7dXHZYCgePqbALusyBSheUWZ46dZ8DQgwDUmdhN5nlOuhtOcWiIXaMzrrVuavYeKxGax71W8tIzO";
    private static final long TEST_EXPIRATION = 1000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "jwtSecretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_EXPIRATION);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("john@technogise.com");
        testUser.setName("John Doe");
        testUser.setRole(UserRole.EMPLOYEE);
    }

    @Test
    void shouldReturnNonNullTokenWhenTokenIsGenerated() {
        // When
        String token = jwtService.generateToken(testUser);

        // Then
        assertNotNull(token);
    }
}