package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.CreateFolderRequest;
import com.pctb.webapp.dto.response.FolderResponse;
import com.pctb.webapp.entity.Folder;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.FolderRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FolderService {
    FolderRepo folderRepo;

    UserRepo userRepo;

    @Transactional
    public FolderResponse createFolder(CreateFolderRequest request, JwtAuthenticationToken authentication) {
        String userId = authentication.getName();
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String folderName = normalizeFolderName(request.getName());
        String parentFolderId = normalizeOptionalId(request.getParentFolderId());
        Folder parent = resolveParentFolder(parentFolderId, userId);

        if (folderRepo.existsActiveByOwnerIdAndParentIdAndName(userId, parentFolderId, folderName)) {
            throw new AppException(ErrorCode.FOLDER_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        Folder folder = Folder.builder()
                .name(folderName)
                .size(0L)
                .owner(owner)
                .parent(parent)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();

        return buildFolderResponse(folderRepo.save(folder));
    }

    public List<FolderResponse> getMyFolders(String parentFolderId, JwtAuthenticationToken authentication) {
        String userId = authentication.getName();
        String normalizedParentFolderId = normalizeOptionalId(parentFolderId);

        if (normalizedParentFolderId != null) {
            resolveParentFolder(normalizedParentFolderId, userId);
        }

        return folderRepo.findActiveByOwnerIdAndParentId(userId, normalizedParentFolderId)
                .stream()
                .map(this::buildFolderResponse)
                .toList();
    }

    private Folder resolveParentFolder(String parentFolderId, String userId) {
        if (parentFolderId == null) {
            return null;
        }

        return folderRepo.findActiveByIdAndOwnerId(parentFolderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FOLDER_NOT_FOUND));
    }

    private String normalizeFolderName(String name) {
        String cleanName = StringUtils.cleanPath(name == null ? "" : name).trim();
        int slashIndex = Math.max(cleanName.lastIndexOf('/'), cleanName.lastIndexOf('\\'));

        if (slashIndex >= 0) {
            cleanName = cleanName.substring(slashIndex + 1).trim();
        }

        if (cleanName.isBlank()) {
            throw new AppException(ErrorCode.FOLDER_NAME_REQUIRED);
        }

        if (cleanName.length() > 100) {
            throw new AppException(ErrorCode.FOLDER_NAME_INVALID);
        }

        return cleanName;
    }

    private String normalizeOptionalId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return id.trim();
    }

    private FolderResponse buildFolderResponse(Folder folder) {
        return FolderResponse.builder()
                .folderId(folder.getId())
                .name(folder.getName())
                .parentFolderId(folder.getParent() == null ? null : folder.getParent().getId())
                .size(folder.getSize())
                .deleted(Boolean.TRUE.equals(folder.getDeleted()))
                .createdAt(folder.getCreatedAt() == null ? null : folder.getCreatedAt().toString())
                .updatedAt(folder.getUpdatedAt() == null ? null : folder.getUpdatedAt().toString())
                .build();
    }
}