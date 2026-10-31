package com.rms.iam.bootstrap;

import com.rms.iam.entity.Role;
import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SysAdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${rms.sysadmin.email:sysadmin@rms.internal}")
    private String sysadminEmail;

    @Value("${rms.sysadmin.initial-password:CHANGE_ME_STRONG_INITIAL_PASSWORD}")
    private String sysadminPassword;

    public SysAdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail(sysadminEmail).isEmpty()) {
            User sysAdmin = new User();
            sysAdmin.setEmail(sysadminEmail);
            sysAdmin.setPasswordHash(passwordEncoder.encode(sysadminPassword));
            sysAdmin.setRole(Role.SYS_ADMIN);
            sysAdmin.setStatus(UserStatus.ACTIVE);
            sysAdmin.setPrivacyAcceptedAt(Instant.now());
            userRepository.save(sysAdmin);
        }
    }
}
