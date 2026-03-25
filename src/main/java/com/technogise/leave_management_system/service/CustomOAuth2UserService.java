package com.technogise.leave_management_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserService userService;
    private final String allowedDomain;

    public CustomOAuth2UserService( UserService userService, @Value("${app.allowed-email-domain}") String allowedDomain)
    {
        this.userService = userService;
        this.allowedDomain = allowedDomain;
    }


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User user = super.loadUser(userRequest);
        String email = user.getAttribute("email");
        String name = user.getAttribute("name");

        if (!isValidEmail(email))
        {
            throw new OAuth2AuthenticationException("Unauthorized email domain");
        }

        userService.findOrCreateUser(email, name);

        return user;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.endsWith(allowedDomain);
    }
}
