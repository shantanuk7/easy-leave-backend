package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final String allowedDomain;
    private final UserService userService;
    private final AnnualLeaveService annualLeaveService;

    public CustomOAuth2UserService(@Value("${app.allowed-email-domain}") String allowedDomain, UserService userService,
                                   AnnualLeaveService annualLeaveService)
    {
        this.allowedDomain = allowedDomain;
        this.userService = userService;
        this.annualLeaveService = annualLeaveService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oauthUser = super.loadUser(userRequest);
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (!isAllowedEmail(email))
        {
            throw new OAuth2AuthenticationException("Unauthorized email domain");
        }

        String accessToken = userRequest.getAccessToken().getTokenValue();
        Instant expiresAt = userRequest.getAccessToken().getExpiresAt();
        User user = userService.findOrCreateUser(email, name, accessToken, expiresAt, null);
        annualLeaveService.createAnnualLeaveForNewEmployee(user);

        return oauthUser;
    }

    private boolean isAllowedEmail(String email) {
        String domain = allowedDomain.startsWith("@") ? allowedDomain : "@" + allowedDomain;
        return email != null && email.endsWith(domain);
    }
}
