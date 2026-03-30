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
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

@ExtendWith(MockitoExtension.class)
@WireMockTest
public class CustomOAuth2UserServiceTest {

    @Mock
    private OAuth2UserRequest userRequest;

    @Mock
    private OAuth2User oAuth2User;

    private CustomOAuth2UserService customOAuth2UserService;

    private static final String ALLOWED_DOMAIN = "@technogise.com";

    @BeforeEach
    void setUp() {
        customOAuth2UserService = spy(new CustomOAuth2UserService(ALLOWED_DOMAIN));
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        doReturn(oAuth2User).when(customOAuth2UserService).fetchOAuth2User(userRequest);
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        assertThrows(
                OAuth2AuthenticationException.class,
                () -> customOAuth2UserService.loadUser(userRequest)
        );
    }

    @Test
    void shouldThrowExceptionWhenEmailIsEmptyString() {
        doReturn(oAuth2User).when(customOAuth2UserService).fetchOAuth2User(userRequest);
        when(oAuth2User.getAttribute("email")).thenReturn("");

        assertThrows(OAuth2AuthenticationException.class, () -> customOAuth2UserService.loadUser(userRequest));
    }

    @Test
    void shouldFetchOAuth2UserWithMockedOAuthServer(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(get("/userinfo")
                .willReturn(okJson("""
                        {
                          "sub": "12345",
                          "email": "rakshit@technogise.com",
                          "name": "Rakshit Saxena"
                        }
                        """)));

        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("google")
                .clientId("fake-client-id")
                .clientSecret("fake-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .authorizationUri("http://localhost/oauth2/authorize")
                .tokenUri("http://localhost/oauth2/token")
                .userInfoUri(wmRuntimeInfo.getHttpBaseUrl() + "/userinfo")
                .userNameAttributeName("sub")
                .build();

        OAuth2UserRequest wireMockUserRequest = new OAuth2UserRequest(clientRegistration,
                new OAuth2AccessToken(BEARER, "fake-token", Instant.now(), Instant.now().plusSeconds(3600)));

        CustomOAuth2UserService service = new CustomOAuth2UserService(ALLOWED_DOMAIN);
        OAuth2User result = service.loadUser(wireMockUserRequest);

        assertEquals("rakshit@technogise.com", result.getAttribute("email"));
    }
}
