package com.pctb.webapp.repository;

import com.pctb.webapp.entity.StoragePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StoragePlanRepo extends JpaRepository<StoragePlan, String> {

    // Tìm các gói có dung lượng lớn hơn hẳn dung lượng hiện tại của user (Logic khống chế bậc thang)
    @Query("SELECT p FROM StoragePlan p WHERE p.quotaSize > :currentQuota ORDER BY p.quotaSize ASC, p.price ASC")
    List<StoragePlan> findAvailableUpgrades(@Param("currentQuota") Long currentQuota);
}