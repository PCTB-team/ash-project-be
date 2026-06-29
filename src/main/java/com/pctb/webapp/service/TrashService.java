package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.DeleteTrashItemsRequest;
import com.pctb.webapp.dto.request.RestoreTrashItemsRequest;
import com.pctb.webapp.dto.response.DeleteTrashItemsResponse;
import com.pctb.webapp.dto.response.RestoreTrashItemsResponse;
import com.pctb.webapp.dto.response.TrashItemResponse;
import com.pctb.webapp.dto.response.TrashResponse;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.Folder;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.FolderRepo;
import com.pctb.webapp.util.DateTimeUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TrashService {
    DocumentRepo documentRepo;
    FolderRepo folderRepo;
    StorageService storageService;
    QdrantService qdrantService;
    DocumentIndexingService documentIndexingService;

    public TrashResponse getMyTrash(JwtAuthenticationToken authentication) {
        String userId = authentication.getName();
        List<TrashItemResponse> items = new ArrayList<>();

        for (Folder folder : folderRepo.findTrashByOwnerId(userId)) {
            items.add(buildFolderTrashItem(folder));
        }

        for (Document document : documentRepo.findTrashByOwnerId(userId)) {
            items.add(buildDocumentTrashItem(document));
        }

        items.sort(Comparator
                .comparing(this::parseDeletedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TrashItemResponse::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

        return TrashResponse.builder()
                .total(items.size())
                .items(items)
                .build();
    }

    @Transactional
    public DeleteTrashItemsResponse deleteTrashItems(
            DeleteTrashItemsRequest request,
            JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getName();
        Set<String> documentIds = normalizeIds(request == null ? null : request.getDocumentIds());
        Set<String> folderIds = normalizeIds(request == null ? null : request.getFolderIds());

        long deletedDocuments = 0;
        long deletedFolders = 0;

        for (String documentId : documentIds) {
            Document document = documentRepo.findById(documentId).orElse(null);
            if (document == null || !userId.equals(document.getOwner().getId())) {
                continue;
            }

            if (!Boolean.TRUE.equals(document.getDeleted())) {
                continue;
            }

            deleteDocumentPermanently(document, userId);
            deletedDocuments++;
        }

        for (String folderId : folderIds) {
            Folder folder = folderRepo.findTrashByIdAndOwnerId(folderId, userId).orElse(null);
            if (folder == null || !folderRepo.existsById(folder.getId())) {
                continue;
            }

            DeleteCounter counter = deleteFolderTreePermanently(folder, userId);
            deletedDocuments += counter.documents();
            deletedFolders += counter.folders();
        }

        return DeleteTrashItemsResponse.builder()
                .deletedDocuments(deletedDocuments)
                .deletedFolders(deletedFolders)
                .totalDeleted(deletedDocuments + deletedFolders)
                .build();
    }

    @Transactional
    public RestoreTrashItemsResponse restoreTrashItems(
            RestoreTrashItemsRequest request,
            JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getName();
        Set<String> documentIds = normalizeIds(request == null ? null : request.getDocumentIds());
        Set<String> folderIds = normalizeIds(request == null ? null : request.getFolderIds());
        LocalDateTime restoredAt = DateTimeUtils.nowUtc();

        long restoredDocuments = 0;
        long restoredFolders = 0;

        for (String folderId : folderIds) {
            Folder folder = folderRepo.findTrashByIdAndOwnerId(folderId, userId).orElse(null);
            if (folder == null) {
                continue;
            }

            if (hasSelectedAncestor(folder, folderIds)) {
                continue;
            }

            RestoreCounter counter = restoreFolderTree(folder, userId, restoredAt);
            restoredDocuments += counter.documents();
            restoredFolders += counter.folders();
            updateParentSizeCascade(folder.getParent(), safeFolderSize(folder), restoredAt);
        }

        for (String documentId : documentIds) {
            Document document = documentRepo.findById(documentId).orElse(null);
            if (document == null || !userId.equals(document.getOwner().getId())) {
                continue;
            }

            if (!Boolean.TRUE.equals(document.getDeleted())) {
                continue;
            }

            restoreDocument(document, restoredAt);
            updateParentSizeCascade(document.getFolder(), safeFileSize(document), restoredAt);
            documentIndexingService.indexDocument(document.getId());
            restoredDocuments++;
        }

        return RestoreTrashItemsResponse.builder()
                .restoredDocuments(restoredDocuments)
                .restoredFolders(restoredFolders)
                .totalRestored(restoredDocuments + restoredFolders)
                .build();
    }

    private TrashItemResponse buildFolderTrashItem(Folder folder) {
        return TrashItemResponse.builder()
                .type("FOLDER")
                .folderId(folder.getId())
                .name(folder.getName())
                .size(folder.getSize())
                .parentFolderId(folder.getParent() == null ? null : folder.getParent().getId())
                .deletedAt(DateTimeUtils.toDisplayDateTime(folder.getDeletedAt()))
                .build();
    }

    private TrashItemResponse buildDocumentTrashItem(Document document) {
        return TrashItemResponse.builder()
                .type("DOCUMENT")
                .documentId(document.getId())
                .name(document.getFileName())
                .size(document.getFileSize())
                .documentFolderId(document.getFolder() == null ? null : document.getFolder().getId())
                .fileExtension(document.getFileExtension())
                .mimeType(document.getMimeType())
                .deletedAt(DateTimeUtils.toDisplayDateTime(document.getDeletedAt()))
                .build();
    }

    private Instant parseDeletedAt(TrashItemResponse item) {
        if (item.getDeletedAt() == null) {
            return null;
        }

        return OffsetDateTime.parse(item.getDeletedAt()).toInstant();
    }

    private Set<String> normalizeIds(List<String> ids) {
        if (ids == null) {
            return Set.of();
        }

        Set<String> normalizedIds = new HashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                normalizedIds.add(id.trim());
            }
        }

        return normalizedIds;
    }

    private DeleteCounter deleteFolderTreePermanently(Folder folder, String userId) {
        long deletedDocuments = 0;
        long deletedFolders = 0;

        for (Folder childFolder : folderRepo.findByOwnerIdAndParentId(userId, folder.getId())) {
            if (!folderRepo.existsById(childFolder.getId())) {
                continue;
            }

            DeleteCounter counter = deleteFolderTreePermanently(childFolder, userId);
            deletedDocuments += counter.documents();
            deletedFolders += counter.folders();
        }

        for (Document document : documentRepo.findByOwnerIdAndFolderId(userId, folder.getId())) {
            if (!documentRepo.existsById(document.getId())) {
                continue;
            }

            deleteDocumentPermanently(document, userId);
            deletedDocuments++;
        }

        folderRepo.delete(folder);
        deletedFolders++;

        return new DeleteCounter(deletedDocuments, deletedFolders);
    }

    private void deleteDocumentPermanently(Document document, String userId) {
        qdrantService.deleteDocumentChunks(userId, document.getId());
        storageService.delete(document.getStorageUrl());
        documentRepo.delete(document);
    }

    private record DeleteCounter(long documents, long folders) {
    }

    private RestoreCounter restoreFolderTree(Folder folder, String userId, LocalDateTime restoredAt) {
        long restoredDocuments = 0;
        long restoredFolders = 0;

        for (Document document : documentRepo.findByOwnerIdAndFolderId(userId, folder.getId())) {
            if (!Boolean.TRUE.equals(document.getDeleted())) {
                continue;
            }

            restoreDocument(document, restoredAt);
            documentIndexingService.indexDocument(document.getId());
            restoredDocuments++;
        }

        for (Folder childFolder : folderRepo.findByOwnerIdAndParentId(userId, folder.getId())) {
            if (!Boolean.TRUE.equals(childFolder.getDeleted())) {
                continue;
            }

            RestoreCounter counter = restoreFolderTree(childFolder, userId, restoredAt);
            restoredDocuments += counter.documents();
            restoredFolders += counter.folders();
        }

        folder.setDeleted(false);
        folder.setDeletedAt(null);
        folder.setUpdatedAt(restoredAt);
        folderRepo.save(folder);
        restoredFolders++;

        return new RestoreCounter(restoredDocuments, restoredFolders);
    }

    private void restoreDocument(Document document, LocalDateTime restoredAt) {
        document.setDeleted(false);
        document.setDeletedAt(null);
        document.setUpdatedAt(restoredAt);
        documentRepo.save(document);
    }

    private void updateParentSizeCascade(Folder folder, long delta, LocalDateTime updatedAt) {
        Folder current = folder;

        while (current != null) {
            long currentSize = current.getSize() == null ? 0 : current.getSize();
            current.setSize(Math.max(0, currentSize + delta));
            current.setUpdatedAt(updatedAt);
            folderRepo.save(current);
            current = current.getParent();
        }
    }

    private long safeFileSize(Document document) {
        return document.getFileSize() == null ? 0 : document.getFileSize();
    }

    private long safeFolderSize(Folder folder) {
        return folder.getSize() == null ? 0 : folder.getSize();
    }

    private boolean hasSelectedAncestor(Folder folder, Set<String> selectedFolderIds) {
        Folder current = folder.getParent();

        while (current != null) {
            if (selectedFolderIds.contains(current.getId())) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }

    private record RestoreCounter(long documents, long folders) {
    }
}
