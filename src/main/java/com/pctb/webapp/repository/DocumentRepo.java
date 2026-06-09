package com.pctb.webapp.repository;

import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepo extends JpaRepository<Document, String> {
    @Query("select d from Document d where d.owner = :owner and d.fileName = :fileName")
    Optional<Document> findByOwnerAndFileName(@Param("owner") User owner, @Param("fileName") String fileName);

    List<Document> findByOwnerOrderByCreatedAtDesc(User owner);



    @Query("""
            select d from Document d
            where d.owner = :owner
              and d.deleted = true
            order by d.deletedAt desc
            """)
    List<Document> findActiveByOwner(@Param("owner") User owner);


    @Query("select d from Document d where d.owner.id = :ownerId and (d.deleted = false or d.deleted is null) order by d.createdAt desc")
    List<Document> findActiveByOwnerId(@Param("ownerId") String ownerId);

    @Query(
            value = "select d from Document d where d.owner.id = :ownerId and (d.deleted = false or d.deleted is null) order by d.createdAt desc",
            countQuery = "select count(d) from Document d where d.owner.id = :ownerId and (d.deleted = false or d.deleted is null)"
    )
    Page<Document> findActiveByOwnerId(@Param("ownerId") String ownerId, Pageable pageable);

    @Query(
            value = "select d from Document d where d.owner.id = :ownerId and ((:folderId is null and d.folder is null) or d.folder.id = :folderId) and (d.deleted = false or d.deleted is null) order by d.createdAt desc",
            countQuery = "select count(d) from Document d where d.owner.id = :ownerId and ((:folderId is null and d.folder is null) or d.folder.id = :folderId) and (d.deleted = false or d.deleted is null)"
    )
    Page<Document> findActiveByOwnerIdAndFolderId(@Param("ownerId") String ownerId, @Param("folderId") String folderId, Pageable pageable);

    @Query("select d from Document d where d.owner.id = :ownerId and d.fileName = :fileName and ((:folderId is null and d.folder is null) or d.folder.id = :folderId)")
    Optional<Document> findByOwnerIdAndFolderIdAndFileName(@Param("ownerId") String ownerId, @Param("folderId") String folderId, @Param("fileName") String fileName);

    @Query("select d from Document d where d.owner = :owner and d.deleted = true order by d.deletedAt desc")
    List<Document> findTrashByOwner(@Param("owner") User owner);

    @Query("select d from Document d where d.owner.id = :ownerId and d.deleted = true order by d.deletedAt desc")
    List<Document> findTrashByOwnerId(@Param("ownerId") String ownerId);

    @Query("select coalesce(sum(d.fileSize), 0) from Document d where d.owner = :owner")
    Long sumFileSizeByOwner(@Param("owner") User owner);

    @Query("select count(d) from Document d where d.owner = :owner and (d.deleted = false or d.deleted is null)")
    long countActiveByOwner(@Param("owner") User owner);
}


