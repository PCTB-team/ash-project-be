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
            if (planRepo.count() > 0) return;

            // --- HỆ THỐNG GÓI BẬC THANG: GO (2GB) ---
            StoragePlan planGo1M = StoragePlan.builder()
                    .id("PLAN_GO_1M").planName("GO Plan - 1 Month (2GB quota)")
                    .quotaSize(2L * 1024 * 1024 * 1024).price(2000L).durationMonths(1).build();

            StoragePlan planGo1Y = StoragePlan.builder()
                    .id("PLAN_GO_1Y").planName("GO Plan - 1 Year (2GB quota) [Discount]")
                    .quotaSize(2L * 1024 * 1024 * 1024).price(20000L).durationMonths(12).build();

            // --- HỆ THỐNG GÓI BẬC THANG: PLUS (5GB) ---
            StoragePlan planPlus1M = StoragePlan.builder()
                    .id("PLAN_PLUS_1M").planName("PLUS Plan - 1 Month (5GB quota)")
                    .quotaSize(5L * 1024 * 1024 * 1024).price(5000L).durationMonths(1).build();

            StoragePlan planPlus1Y = StoragePlan.builder()
                    .id("PLAN_PLUS_1Y").planName("PLUS Plan - 1 Year (5GB quota) [Discount]")
                    .quotaSize(5L * 1024 * 1024 * 1024).price(50000L).durationMonths(12).build();

            // --- HỆ THỐNG GÓI BẬC THANG: PRO (10GB) ---
            StoragePlan planPro1M = StoragePlan.builder()
                    .id("PLAN_PRO_1M").planName("PRO Plan - 1 Month (10GB quota)")
                    .quotaSize(10L * 1024 * 1024 * 1024).price(10000L).durationMonths(1).build();

            StoragePlan planPro1Y = StoragePlan.builder()
                    .id("PLAN_PRO_1Y").planName("PRO Plan - 1 Year (10GB quota) [Discount]")
                    .quotaSize(10L * 1024 * 1024 * 1024).price(100000L).durationMonths(12).build();

            planRepo.save(planGo1M); planRepo.save(planGo1Y);
            planRepo.save(planPlus1M); planRepo.save(planPlus1Y);
            planRepo.save(planPro1M); planRepo.save(planPro1Y);

            log.info("[SWP391] Six tiered storage plans have been initialized successfully.");
        };
    }
}
