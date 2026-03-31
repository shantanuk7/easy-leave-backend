package com.technogise.leave_management_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final String allowedDomain;
    private final UserService userService;

    public CustomOAuth2UserService(@Value("${app.allowed-email-domain}") String allowedDomain, UserService userService)
    {
        this.allowedDomain = allowedDomain;
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User user = super.loadUser(userRequest);
        String email = user.getAttribute("email");
        String name = user.getAttribute("name");

        if (!isAllowedEmail(email))
        {
            throw new OAuth2AuthenticationException("Unauthorized email domain");
        }

        userService.findOrCreateUser(email, name);

        return user;
    }

    private boolean isAllowedEmail(String email) {
        return email != null && email.endsWith(allowedDomain);
    }
}
