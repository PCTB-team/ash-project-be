package com.pctb.webapp.ai.service;

import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.Folder;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.JoinStatus;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.FolderRepo;
import com.pctb.webapp.repository.GroupMemberRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

/**
 * Tap trung cac rule phan quyen cho AI.
 * Moi lan AI truy cap document/group deu phai di qua service nay.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiPermissionService {
    DocumentRepo documentRepo;

    FolderRepo folderRepo;

    GroupMemberRepo groupMemberRepo;

    /**
     * Dam bao document ton tai, thuoc user hien tai va chua bi move vao trash.
     */
    public Document requireOwnedActiveDocument(String documentId, String userId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getOwner().getId().equals(userId) || Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        return document;
    }

    /**
     * Dam bao folder ton tai, thuoc user hien tai va chua bi xoa.
     */
    public Folder requireOwnedActiveFolder(String folderId, String userId) {
        return folderRepo.findActiveByIdAndOwnerId(folderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FOLDER_NOT_FOUND));
    }

    /**
     * Dam bao user la member APPROVED cua group truoc khi chat/index group file.
     */
    public GroupMember requireApprovedGroupMember(String groupId, String userId) {
        GroupMember member = groupMemberRepo.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_ACCESS_DENIED));

        if (member.getJoinStatus() != JoinStatus.APPROVED) {
            throw new AppException(ErrorCode.GROUP_MEMBER_NOT_APPROVED);
        }

        return member;
    }
}
