package com.pctb.webapp.repository;

import com.pctb.webapp.entity.StoragePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * INTERFACE REPOSITORY QUẢN LÝ TRUY VẤN GÓI DUNG LƯỢNG
 * Kế thừa JpaRepository với <Tên_Entity, Kiểu_Dữ_Liệu_Của_Khóa_Chính>
 */
@Repository
public interface StoragePlanRepo extends JpaRepository<StoragePlan, String> {
}