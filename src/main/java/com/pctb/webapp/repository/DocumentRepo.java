package com.pctb.webapp.repository;

import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepo extends JpaRepository<Document, String> {
    boolean existsByOwnerAndFileName(User owner, String fileName);

    Optional<Document> findByOwnerAndFileName(User owner, String fileName);

    List<Document> findByOwnerOrderByCreatedAtDesc(User owner);

    @Query("select d from Document d where d.owner = :owner and (d.deleted = false or d.deleted is null) order by d.createdAt desc")
    List<Document> findActiveByOwner(@Param("owner") User owner);

    @Query("select d from Document d where d.owner = :owner and d.deleted = true order by d.deletedAt desc")
    List<Document> findTrashByOwner(@Param("owner") User owner);

    @Query("select coalesce(sum(d.fileSize), 0) from Document d where d.owner = :owner")
    Long sumFileSizeByOwner(@Param("owner") User owner);
}
