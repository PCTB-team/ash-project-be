package com.pctb.webapp.configuration;
import com.pctb.webapp.entity.Role;
import com.pctb.webapp.entity.RoleEnum;
import com.pctb.webapp.entity.StoragePlan;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.repository.RoleRepo;
import com.pctb.webapp.repository.StoragePlanRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

// Class dùng để tạo account admin khi chạy
public class InitConfig {
    final UserRepo userRepo;
    final RoleRepo roleRepo;
    final PasswordEncoder passwordEncoder;
    final StoragePlanRepo planRepo; // Khai báo thêm để gọi tầng lưu gói dịch vụ

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
                    .storageQuota(5368709120L) // Đồng bộ tránh lỗi null bộ nhớ cho Admin
                    .storageUsed(0L)
                    .roles(Set.of(adminRole))
                    .build();

            userRepo.save(admin);

            System.out.println("Admin account created: " + adminEmail);


        };
    }

    @Bean
    CommandLineRunner initStoragePlans() {
        return args -> {

            if (planRepo.count() > 0) {
                return;
            }

            StoragePlan plan5gb = StoragePlan.builder()
                    .id("PLAN_5GB")
                    .planName("Gói Tăng Tốc Bộ Nhớ 5GB")
                    .quotaSize(5L * 1024 * 1024 * 1024)
                    .price(2000L)
                    .build();

            StoragePlan plan10gb = StoragePlan.builder()
                    .id("PLAN_10GB")
                    .planName("Gói Mở Rộng Bộ Nhớ 10GB")
                    .quotaSize(10L * 1024 * 1024 * 1024)
                    .price(3000L)
                    .build();

            StoragePlan plan50gb = StoragePlan.builder()
                    .id("PLAN_50GB")
                    .planName("Gói Dung Lượng Khổng Lồ 50GB")
                    .quotaSize(50L * 1024 * 1024 * 1024)
                    .price(4000L)
                    .build();

            planRepo.save(plan5gb);
            planRepo.save(plan10gb);
            planRepo.save(plan50gb);

            log.info("[SWP391] Storage plans seeded successfully");
        };
    }
}
