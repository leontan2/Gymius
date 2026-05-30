package com.gymius.controller;

import com.gymius.dto.UserProfileDto;
import com.gymius.service.UserAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserAccountService userAccountService;

    public MeController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public UserProfileDto me(@AuthenticationPrincipal OidcUser oidcUser) {
        return userAccountService.toProfile(userAccountService.syncFromGoogle(oidcUser));
    }
}
