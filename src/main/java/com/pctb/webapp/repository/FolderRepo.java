package com.pctb.webapp.repository;

import com.pctb.webapp.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepo extends JpaRepository<Folder, String> {
    @Query("select f from Folder f where f.id = :folderId and f.owner.id = :ownerId and (f.deleted = false or f.deleted is null)")
    Optional<Folder> findActiveByIdAndOwnerId(@Param("folderId") String folderId, @Param("ownerId") String ownerId);

    @Query("select f from Folder f where f.id = :folderId and f.owner.id = :ownerId and f.deleted = true")
    Optional<Folder> findTrashByIdAndOwnerId(@Param("folderId") String folderId, @Param("ownerId") String ownerId);

    @Query("select f from Folder f where f.owner.id = :ownerId and ((:parentId is null and f.parent is null) or f.parent.id = :parentId) and (f.deleted = false or f.deleted is null) order by f.createdAt desc")
    List<Folder> findActiveByOwnerIdAndParentId(@Param("ownerId") String ownerId, @Param("parentId") String parentId);

    @Query("select f from Folder f where f.owner.id = :ownerId and f.parent.id = :parentId order by f.createdAt desc")
    List<Folder> findByOwnerIdAndParentId(@Param("ownerId") String ownerId, @Param("parentId") String parentId);

    @Query("select f from Folder f where f.owner.id = :ownerId and f.deleted = true order by f.deletedAt desc")
    List<Folder> findTrashByOwnerId(@Param("ownerId") String ownerId);

    @Query("select count(f) > 0 from Folder f where f.owner.id = :ownerId and lower(f.name) = lower(:name) and ((:parentId is null and f.parent is null) or f.parent.id = :parentId) and (f.deleted = false or f.deleted is null)")
    boolean existsActiveByOwnerIdAndParentIdAndName(@Param("ownerId") String ownerId, @Param("parentId") String parentId, @Param("name") String name);
}
