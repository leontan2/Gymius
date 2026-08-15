package com.gymius.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("local")
public class DevAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.auth.dev-bypass-enabled:false}")
    private boolean enabled;

    @Value("${app.auth.dev-subject:local-dev-user}")
    private String subject;

    @Value("${app.auth.dev-email:local@gymius.dev}")
    private String email;

    @Value("${app.auth.dev-name:Local Gymius User}")
    private String name;

    @Value("${app.auth.dev-picture:}")
    private String picture;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(devAuthentication(request));
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken devAuthentication(HttpServletRequest request) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put(IdTokenClaimNames.SUB, subject);
        claims.put(StandardClaimNames.EMAIL, email);
        claims.put(StandardClaimNames.NAME, name);

        if (!picture.isBlank()) {
            claims.put(StandardClaimNames.PICTURE, picture);
        }

        OidcIdToken idToken = new OidcIdToken("local-dev-token", now, now.plusSeconds(3600), claims);
        DefaultOidcUser principal = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                idToken
        );
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                idToken.getTokenValue(),
                principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }
}
