package com.cybermanager.auth.domain.port;

import com.cybermanager.auth.domain.model.EmailAddress;
import com.cybermanager.auth.domain.model.UserAccount;
import com.cybermanager.auth.domain.model.UserId;

import java.util.Optional;

public interface UserAuthenticationRepository {
    Optional<UserAccount> findByEmail(EmailAddress email);

    Optional<UserAccount> findById(UserId id);
}

