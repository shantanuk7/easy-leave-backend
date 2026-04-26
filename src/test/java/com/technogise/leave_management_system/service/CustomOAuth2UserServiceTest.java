package com.technogise.leave_management_system.service;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.time.Instant;
import java.util.Map;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

@ExtendWith(MockitoExtension.class)
@WireMockTest
public class CustomOAuth2UserServiceTest {
    @Mock
    private UserService userService;
    @Mock
    private AnnualLeaveService annualLeaveService;

    private CustomOAuth2UserService customOAuth2UserService;
    private static final String ALLOWED_DOMAIN = "@technogise.com";

    @BeforeEach
    void setUp() {
        customOAuth2UserService = new CustomOAuth2UserService(ALLOWED_DOMAIN, userService,  annualLeaveService);
    }

    private OAuth2UserRequest buildRequest(WireMockRuntimeInfo wm) {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("google")
                .clientId("fake-client-id")
                .clientSecret("fake-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .authorizationUri("http://localhost/oauth2/authorize")
                .tokenUri("http://localhost/oauth2/token")
                .userInfoUri(wm.getHttpBaseUrl() + "/userinfo")
                .userNameAttributeName("sub")
                .build();

        return new OAuth2UserRequest(clientRegistration,
                new OAuth2AccessToken(BEARER, "fake-token",
                        Instant.now(), Instant.now().plusSeconds(3600)),
                Map.of("refresh_token", "mock-refresh-token")
        );
    }


    @Test
    void shouldThrowExceptionWhenEmailIsEmptyString(WireMockRuntimeInfo wm) {
        stubFor(get("/userinfo").willReturn(okJson("""
                { "sub": "01", "email": "", "name": "Rakshit" }
                """)));

        assertThrows(OAuth2AuthenticationException.class,
                () -> customOAuth2UserService.loadUser(buildRequest(wm)));
    }

    @Test
    void shouldThrowExceptionWhenEmailDomainIsNotAllowed(WireMockRuntimeInfo wm) {
        stubFor(get("/userinfo").willReturn(okJson("""
                { "sub": "01", "email": "rakshit@gmail.com", "name": "Rakshit" }
                """)));

        assertThrows(OAuth2AuthenticationException.class, () -> customOAuth2UserService.loadUser(buildRequest(wm)));
    }

    @Test
    void shouldThrowExceptionWhenEmailDomainContainsAllowedDomainAsSubstring(WireMockRuntimeInfo wm) {
        customOAuth2UserService = new CustomOAuth2UserService("technogise.com", userService,  annualLeaveService);

        stubFor(get("/userinfo").willReturn(okJson("""
            { "sub": "01", "email": "rakshit@hackertechnogise.com", "name": "Rakshit" }
            """)));

        assertThrows(OAuth2AuthenticationException.class,
                () -> customOAuth2UserService.loadUser(buildRequest(wm)));
    }

    @Test
    void shouldReturnUserWhenEmailDomainIsAllowed(WireMockRuntimeInfo wm) {
        stubFor(get("/userinfo").willReturn(okJson("""
                { "sub": "123", "email": "rakshit@technogise.com", "name": "Rakshit Saxena" }""")));

        OAuth2User result = customOAuth2UserService.loadUser(buildRequest(wm));

        assertEquals("rakshit@technogise.com", result.getAttribute("email"));
    }
    @Test
    void shouldThrowExceptionWhenEmailIsNull(WireMockRuntimeInfo wm) {
        stubFor(get("/userinfo").willReturn(okJson("""
                 { "sub": "01", "name": "Rakshit" }""")));

        assertThrows(OAuth2AuthenticationException.class,
                () -> customOAuth2UserService.loadUser(buildRequest(wm)));
    }

    @Test
    void shouldExtractRefreshTokenWhenPresent(WireMockRuntimeInfo wm) {
        stubFor(get("/userinfo").willReturn(okJson("""
            { "sub": "123", "email": "priyansh@technogise.com", "name": "Priyansh" }
            """)));

        OAuth2User result = customOAuth2UserService.loadUser(
                buildRequest(wm)
        );
        verify(userService).findOrCreateUser(
                eq("priyansh@technogise.com"),
                eq("Priyansh"),
                eq("fake-token"),
                any(Instant.class),
                eq("mock-refresh-token")
        );
    }
    @Test
    void shouldPassNullWhenRefreshTokenNotPresent(WireMockRuntimeInfo wm) {
        stubFor(get("/userinfo").willReturn(okJson("""
            { "sub": "123", "email": "priyansh@technogise.com", "name": "Priyansh" }
            """)));

        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("google")
                .clientId("fake-client-id")
                .clientSecret("fake-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .authorizationUri("http://localhost/oauth2/authorize")
                .tokenUri("http://localhost/oauth2/token")
                .userInfoUri(wm.getHttpBaseUrl() + "/userinfo")
                .userNameAttributeName("sub")
                .build();

        OAuth2UserRequest request = new OAuth2UserRequest(
                clientRegistration,
                new OAuth2AccessToken(BEARER, "fake-token",
                        Instant.now(), Instant.now().plusSeconds(3600))
        );

        customOAuth2UserService.loadUser(request);

        verify(userService).findOrCreateUser(
                eq("priyansh@technogise.com"),
                eq("Priyansh"),
                eq("fake-token"),
                any(Instant.class),
                eq(null)
        );
    }
}
