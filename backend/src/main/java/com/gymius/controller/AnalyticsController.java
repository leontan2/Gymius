package com.gymius.controller;

import com.gymius.domain.UserAccount;
import com.gymius.dto.DashboardDto;
import com.gymius.dto.PersonalRecordDto;
import com.gymius.dto.ProgressSeriesDto;
import com.gymius.service.AnalyticsService;
import com.gymius.service.UserAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserAccountService userAccountService;

    public AnalyticsController(AnalyticsService analyticsService, UserAccountService userAccountService) {
        this.analyticsService = analyticsService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard(@AuthenticationPrincipal OidcUser oidcUser) {
        return analyticsService.dashboard(currentUser(oidcUser));
    }

    @GetMapping("/progress")
    public List<ProgressSeriesDto> progress(@AuthenticationPrincipal OidcUser oidcUser) {
        return analyticsService.progress(currentUser(oidcUser));
    }

    @GetMapping("/personal-records")
    public List<PersonalRecordDto> personalRecords(@AuthenticationPrincipal OidcUser oidcUser) {
        return analyticsService.personalRecords(currentUser(oidcUser));
    }

    private UserAccount currentUser(OidcUser oidcUser) {
        return userAccountService.syncFromGoogle(oidcUser);
    }
}
