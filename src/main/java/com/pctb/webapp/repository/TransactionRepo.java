package com.pctb.webapp.repository;

import com.pctb.webapp.entity.Transaction;
import com.pctb.webapp.entity.TransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
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

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
}
