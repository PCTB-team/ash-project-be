package com.pctb.webapp.repository;

import com.pctb.webapp.entity.StoragePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * INTERFACE REPOSITORY QUẢN LÝ TRUY VẤN GÓI DUNG LƯỢNG
 * Kế thừa JpaRepository với <Tên_Entity, Kiểu_Dữ_Liệu_Của_Khóa_Chính>
 */
@Repository
public interface StoragePlanRepo extends JpaRepository<StoragePlan, String> {

    // =========================================================================
    // KHU VỰC BỔ SUNG: LỌC CÁC GÓI CÓ DUNG LƯỢNG CAO HƠN MỨC HIỆN TẠI
    // =========================================================================
    /**
     * Spring Data JPA sẽ tự động phân tích cú pháp tên hàm (Query Method) này
     * thành câu lệnh SQL: SELECT * FROM storage_plan WHERE quota_size > :currentQuota
     */
    List<StoragePlan> findByQuotaSizeGreaterThan(Long currentQuota);
}