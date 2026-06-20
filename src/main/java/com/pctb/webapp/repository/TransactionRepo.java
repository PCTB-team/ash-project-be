package com.pctb.webapp.repository;

import com.pctb.webapp.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, String> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") String id);

    /**
     * SỬA LỖI: Chuyển 'long' thành 'Long' để đồng bộ với kiểu dữ liệu của Entity
     */
    Optional<Transaction> findByOrderCode(Long orderCode);

    // --- BỔ SUNG CÁC HÀM PHỤC VỤ DASHBOARD ---

    // --- SỬA LỖI & PHỤC VỤ DASHBOARD ---

    // 1. Tính tổng doanh thu từ các giao dịch thành công (Sử dụng Long thay vì double cho đồng bộ)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'SUCCESS'")
    Long sumTotalRevenue();

    // 2. Đếm tổng số giao dịch theo trạng thái thành công
    long countByStatus(com.pctb.webapp.entity.TransactionStatus status);

    // 3. SỬA LỖI CONFLICT: Thay t.storagePlan thành t.plan để khớp với trường trong Entity Transaction
    @Query("SELECT t.plan.planName, SUM(t.amount) FROM Transaction t WHERE t.status = 'SUCCESS' GROUP BY t.plan.planName")
    List<Object[]> getRevenueGroupedByPackageRaw();
}