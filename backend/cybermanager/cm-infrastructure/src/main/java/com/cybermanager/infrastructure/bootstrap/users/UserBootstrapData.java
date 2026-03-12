package com.cybermanager.infrastructure.bootstrap.users;

import com.cybermanager.infrastructure.entities.persistence.users.AppRoleJpa;
import com.cybermanager.infrastructure.entities.persistence.users.UserJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.users.UserStatusJpa;
import com.cybermanager.infrastructure.repositories.persistence.users.UserJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;
import java.util.UUID;

@Configuration
public class UserBootstrapData {
    @Bean
    CommandLineRunner bootstrapUsers(UserJpaRepository userJpaRepository) {
        return args -> {
            if (userJpaRepository.findByEmail("admin@cybermanager.local").isPresent()) {
                return;
            }
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            UserJpaEntity admin = new UserJpaEntity();
            admin.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            admin.setEmail("admin@cybermanager.local");
            admin.setFirstName("Admin");
            admin.setLastName("CyberManager");
            admin.setPasswordHash(encoder.encode("admin123"));
            admin.setStatus(UserStatusJpa.ACTIVE);
            admin.setRoles(Set.of(AppRoleJpa.ADMIN, AppRoleJpa.EMPLOYEE));
            userJpaRepository.save(admin);
        };
    }
}

