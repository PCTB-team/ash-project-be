package com.pctb.webapp.configuration;
import com.pctb.webapp.entity.Role;
import com.pctb.webapp.entity.RoleEnum;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.repository.RoleRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

// Class dùng để tạo account admin khi chạy
public class InitConfig {
    final UserRepo userRepo;
    final RoleRepo roleRepo;
    final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    String adminUsername;

    @Value("${app.admin.email}")
    String adminEmail;

    @Value("${app.admin.password}")
    String adminPassword;

    @Value("${app.admin.fullname}")
    String adminFullname;

    // Khi app bắt đầu chạy sẽ khởi tạo account admin đã được quy định username, password trong env
    @Bean
    CommandLineRunner initAdminAccount() {
        return args -> {
            Role adminRole = roleRepo.findById(RoleEnum.ADMIN.name())
                    .orElseGet(() -> roleRepo.save(
                            new Role(RoleEnum.ADMIN.name(), "Admin role")
                    ));

            roleRepo.findById(RoleEnum.USER.name())
                    .orElseGet(() -> roleRepo.save(
                            new Role(RoleEnum.USER.name(), "User role")
                    ));

            if (adminPassword == null || adminPassword.isBlank()) {
                System.out.println("Admin account was not created because ADMIN_PASSWORD is empty");
                return;
            }

            if (userRepo.existsByUsername(adminUsername) || userRepo.existsByEmail(adminEmail)) {
                return;
            }

            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .fullname(adminFullname)
                    .verified(true)
                    .roles(Set.of(adminRole))
                    .build();

            userRepo.save(admin);

            System.out.println("Admin account created: " + adminEmail);
        };
    }
}
