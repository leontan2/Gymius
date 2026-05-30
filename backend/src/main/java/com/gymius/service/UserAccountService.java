package com.gymius.service;

import com.gymius.domain.UserAccount;
import com.gymius.dto.UserProfileDto;
import com.gymius.repository.UserAccountRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public UserAccount syncFromGoogle(OidcUser oidcUser) {
        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();

        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new BadCredentialsException("Google account did not provide a valid subject and email.");
        }

        return userAccountRepository.findByGoogleSubject(subject)
                .map(existing -> updateUser(existing, oidcUser))
                .orElseGet(() -> createUser(subject, oidcUser));
    }

    @Transactional(readOnly = true)
    public UserProfileDto toProfile(UserAccount user) {
        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPictureUrl()
        );
    }

    private UserAccount createUser(String subject, OidcUser oidcUser) {
        UserAccount user = new UserAccount();
        user.setGoogleSubject(subject);
        return userAccountRepository.save(updateUser(user, oidcUser));
    }

    private UserAccount updateUser(UserAccount user, OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        String fullName = oidcUser.getFullName();
        String picture = oidcUser.getPicture();

        user.setEmail(email);
        user.setName(fullName == null || fullName.isBlank() ? email : fullName);
        user.setPictureUrl(picture);
        return user;
    }
}
