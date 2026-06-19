package com.pctb.webapp.service;

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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TrashService {
    DocumentRepo documentRepo;
    FolderRepo folderRepo;

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
}
