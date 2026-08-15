package com.gymius.service;

import com.gymius.domain.UserAccount;
import com.gymius.dto.UserProfileDto;
import com.gymius.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final TransactionTemplate transactionTemplate;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.userAccountRepository = userAccountRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public UserAccount syncFromGoogle(OidcUser oidcUser) {
        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();

        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new BadCredentialsException("Google account did not provide a valid subject and email.");
        }

        try {
            return inTransaction(() -> userAccountRepository.findByGoogleSubject(subject)
                    .map(existing -> updateUser(existing, oidcUser))
                    .orElseGet(() -> createUser(subject, oidcUser)));
        } catch (DataIntegrityViolationException conflict) {
            // Two first requests can race before the account exists. The losing insert is
            // rolled back, then resolved in a fresh transaction after the winning commit.
            return inTransaction(() -> userAccountRepository.findByGoogleSubject(subject)
                    .map(existing -> updateUser(existing, oidcUser))
                    .orElseThrow(() -> conflict));
        }
    }

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
        return userAccountRepository.saveAndFlush(updateUser(user, oidcUser));
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

    private <T> T inTransaction(java.util.function.Supplier<T> callback) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> callback.get()));
    }
}
