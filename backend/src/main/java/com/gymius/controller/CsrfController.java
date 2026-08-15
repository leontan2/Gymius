package com.gymius.controller;

import com.gymius.dto.CsrfTokenDto;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/csrf")
public class CsrfController {

    @GetMapping
    public CsrfTokenDto csrf(CsrfToken csrfToken) {
        return new CsrfTokenDto(csrfToken.getToken());
    }
}
