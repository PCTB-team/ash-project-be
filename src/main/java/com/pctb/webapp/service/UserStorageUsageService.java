package com.pctb.webapp.service;

import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.GroupFileRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserStorageUsageService {
    DocumentRepo documentRepo;
    GroupFileRepo groupFileRepo;

    // Tổng dung lượng gồm tài liệu cá nhân và các file mà user đã upload vào group.
    public long getUsedStorage(User user) {
        long documentStorage = safeSize(documentRepo.sumFileSizeByOwner(user));
        long groupFileStorage = safeSize(groupFileRepo.sumFileSizeByUploader(user));
        return documentStorage + groupFileStorage;
    }

    // Ưu tiên quota theo gói của user; chỉ dùng cấu hình mặc định khi DB chưa có quota.
    public long getStorageQuota(User user, long defaultQuota) {
        return user.getStorageQuota() == null ? defaultQuota : user.getStorageQuota();
    }

    // Kiểm tra dung lượng dự kiến trước khi upload hoặc ghi đè file.
    public void validateCapacity(User user, long replacedFileSize, long newFileSize, long defaultQuota) {
        long usedStorage = getUsedStorage(user);
        long projectedStorage = usedStorage - Math.max(0, replacedFileSize) + Math.max(0, newFileSize);

        if (projectedStorage > getStorageQuota(user, defaultQuota)) {
            throw new AppException(ErrorCode.STORAGE_NOT_ENOUGH);
        }
    }

    private long safeSize(Long size) {
        return size == null ? 0 : size;
    }
}
