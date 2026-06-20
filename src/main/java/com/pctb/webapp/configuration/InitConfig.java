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
public class InitConfig {

    // Sử dụng cơ chế Lombok @RequiredArgsConstructor để tự động inject qua Constructor
    final UserRepo userRepo;
    final RoleRepo roleRepo;
    final PasswordEncoder passwordEncoder;
    final StoragePlanRepo planRepo; // Sử dụng trường thuộc tính này xuyên suốt class để hết báo vàng

    @Value("${app.admin.username}")
    String adminUsername;

    @Value("${app.admin.email}")
    String adminEmail;

    @Value("${app.admin.password}")
    String adminPassword;

    @Value("${app.admin.fullname}")
    String adminFullname;

    @Bean
    CommandLineRunner initAdminAccount() {
        // Dùng dấu gạch dưới "_" hoặc @SuppressWarnings để báo cho IntelliJ biết không cần check "args"
        return _ -> {
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
                    .storageQuota(524288000L) // Đồng bộ mặc định ban đầu là 500MB
                    .storageUsed(0L)
                    .roles(Set.of(adminRole))
                    .build();

            userRepo.save(admin);
            System.out.println("Admin account created: " + adminEmail);
        };
    }

    @Bean
    CommandLineRunner initStoragePlans() {
        // Sử dụng trường "planRepo" của class để tránh conflict tham số hàm
        return _ -> {
            if (planRepo.count() > 0) {
                return; // Nếu đã có gói cước trong DB thì không ghi đè
            }

            // =========================================================================
            // GÓI GO - HẠN MỨC 2GB (2 * 1024 * 1024 * 1024 Bytes)
            // =========================================================================
            StoragePlan planGo1Month = StoragePlan.builder()
                    .id("PLAN_GO_1M")
                    .planName("Gói GO - 1 Tháng (Hạn mức 2GB)")
                    .quotaSize(2L * 1024 * 1024 * 1024)
                    .price(2000L)
                    .durationMonths(1)
                    .build();

            StoragePlan planGo1Year = StoragePlan.builder()
                    .id("PLAN_GO_1Y")
                    .planName("Gói GO - 1 Năm (Hạn mức 2GB) [Ưu đãi]")
                    .quotaSize(2L * 1024 * 1024 * 1024)
                    .price(20000L)
                    .durationMonths(12)
                    .build();

            // =========================================================================
            // GÓI PLUS - HẠN MỨC 5GB (5 * 1024 * 1024 * 1024 Bytes)
            // =========================================================================
            StoragePlan planPlus1Month = StoragePlan.builder()
                    .id("PLAN_PLUS_1M")
                    .planName("Gói PLUS - 1 Tháng (Hạn mức 5GB)")
                    .quotaSize(5L * 1024 * 1024 * 1024)
                    .price(5000L)
                    .durationMonths(1)
                    .build();

            StoragePlan planPlus1Year = StoragePlan.builder()
                    .id("PLAN_PLUS_1Y")
                    .planName("Gói PLUS - 1 Năm (Hạn mức 5GB) [Ưu đãi]")
                    .quotaSize(5L * 1024 * 1024 * 1024)
                    .price(50000L)
                    .durationMonths(12)
                    .build();

            // =========================================================================
            // GÓI PRO - HẠN MỨC 10GB (10 * 1024 * 1024 * 1024 Bytes)
            // =========================================================================
            StoragePlan planPro1Month = StoragePlan.builder()
                    .id("PLAN_PRO_1M")
                    .planName("Gói PRO - 1 Tháng (Hạn mức 10GB)")
                    .quotaSize(10L * 1024 * 1024 * 1024)
                    .price(10000L)
                    .durationMonths(1)
                    .build();

            StoragePlan planPro1Year = StoragePlan.builder()
                    .id("PLAN_PRO_1Y")
                    .planName("Gói PRO - 1 Năm (Hạn mức 10GB) [Ưu đãi]")
                    .quotaSize(10L * 1024 * 1024 * 1024)
                    .price(100000L)
                    .durationMonths(12)
                    .build();

            // Lưu toàn bộ 6 gói cước một cách hợp lệ vào DB qua trường thuộc tính "planRepo" của class
            planRepo.save(planGo1Month);
            planRepo.save(planGo1Year);
            planRepo.save(planPlus1Month);
            planRepo.save(planPlus1Year);
            planRepo.save(planPro1Month);
            planRepo.save(planPro1Year);

            log.info("[SWP391] Hệ thống cước VIP (GO, PLUS, PRO) gồm 6 gói đã được đồng bộ thành công!");
        };
    }
}