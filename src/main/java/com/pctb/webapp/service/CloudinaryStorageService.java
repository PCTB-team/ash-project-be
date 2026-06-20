package com.pctb.webapp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryStorageService implements StorageService {

    Cloudinary cloudinary;
    UserRepo userRepo;

    static long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    static Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    // =========================================================================
    // KHU VỰC CHỐT CHẶN: KIỂM TRA HẠN MỨC DUNG LƯỢNG & THỜI HẠN GÓI
    // =========================================================================
    @Transactional
    public void checkStorageBeforeUpload(User user, long newFileSize) {
        long currentQuota = user.getStorageQuota() == null ? 524288000L : user.getStorageQuota();

        // 1. KIỂM TRA THỜI HẠN: Nếu có ngày hết hạn và ngày đó đã qua so với hiện tại
        if (user.getStorageExpiredAt() != null && user.getStorageExpiredAt().isBefore(LocalDateTime.now())) {

            // Tự động phạt: Đặt lại hạn mức về 500MB mặc định ban đầu
            currentQuota = 524288000L;
            user.setStorageQuota(currentQuota);
            user.setStorageExpiredAt(null); // Reset trạng thái hết hạn thành vĩnh viễn (mức cơ bản)
            userRepo.save(user);

            log.warn("Tài khoản [{}] đã quá hạn gói VIP. Hệ thống tự động phạt, hạ hạn mức về 500MB.", user.getUsername());
        }

        // 2. KIỂM TRA DUNG LƯỢNG: Tổng dung lượng cũ + file mới xem có vượt ngưỡng cho phép không
        long storageUsed = user.getStorageUsed() == null ? 0L : user.getStorageUsed();
        if (storageUsed + newFileSize > currentQuota) {
            throw new AppException(ErrorCode.STORAGE_NOT_ENOUGH);
        }
    }

    // =========================================================================
    // CẬP NHẬT: UPLOAD FILE TÀI LIỆU (Sửa lỗi trùng lặp ghi đè public_id)
    // =========================================================================
    @Transactional
    public String upload(MultipartFile file, String fileName, User user) {
        // 1. Kiểm tra dung lượng đầu vào trước khi tốn băng thông đẩy lên Cloudinary
        long newFileSize = file.getSize();
        checkStorageBeforeUpload(user, newFileSize);

        try {
            // Loại bỏ phần mở rộng đuôi file (.pdf, .png...) nếu có trong fileName
            String cleanName = fileName;
            if (cleanName.contains(".")) {
                cleanName = cleanName.substring(0, cleanName.lastIndexOf('.'));
            }

            // SỬA LỖI GHI ĐÈ CHÉO: Tạo cấu trúc folder riêng cho mỗi user kèm chuỗi UUID duy nhất
            String uniquePublicId = "documents/" + user.getId() + "/" + UUID.randomUUID() + "_" + cleanName;

            Map params = ObjectUtils.asMap(
                    "public_id", uniquePublicId,
                    "resource_type", "auto",
                    "overwrite", true
            );

            // 2. Đẩy file thật lên hệ thống Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            String secureUrl = (String) uploadResult.get("secure_url");

            // 3. Tăng dung lượng thực tế đã sử dụng (storageUsed) của user lên
            long currentUsed = user.getStorageUsed() == null ? 0L : user.getStorageUsed();
            user.setStorageUsed(currentUsed + newFileSize);
            userRepo.save(user);

            return secureUrl;
        } catch (IOException exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    // Giữ hàm upload cũ không có tham số User để giữ tính tương thích Interface gốc
    @Override
    @Deprecated
    public String upload(MultipartFile file, String fileName) {
        throw new UnsupportedOperationException("Vui lòng sử dụng hàm upload có tham số User để hệ thống kiểm tra dung lượng.");
    }

    // Xóa tài liệu khỏi Cloudinary dựa trên URL đã lưu; bỏ qua nếu URL rỗng.
    @Override
    public void delete(String storageUrl) {
        if (storageUrl == null || storageUrl.isBlank()) {
            return;
        }
        try {
            String publicId = extractPublicId(storageUrl);
            String resourceType = determinateResourceType(storageUrl);

            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));

            // LƯU Ý: Dung lượng thực tế (storageUsed) nên được trừ đi ở FileService/DocumentService
            // trước khi thực hiện xóa hẳn link cất trữ này để bảo đảm tính chính xác dữ liệu.
        } catch (IOException exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    // Đổi tên hoặc đổi đường dẫn file trên Cloudinary và trả về secure_url mới.
    @Override
    public String rename(String oldStorageUrl, String newFileName) {
        try {
            String oldPublicId = extractPublicId(oldStorageUrl);
            String newPublicId = newFileName;
            if (newPublicId.contains(".")) {
                newPublicId = newPublicId.substring(0, newPublicId.lastIndexOf('.'));
            }

            String resourceType = determinateResourceType(oldStorageUrl);

            Map renameResult = cloudinary.uploader().rename(oldPublicId, newPublicId,
                    ObjectUtils.asMap("resource_type", resourceType, "overwrite", true));

            return (String) renameResult.get("secure_url");
        } catch (IOException exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    @Override
    public Resource loadAsResource(String storageUrl) {
        if (storageUrl == null || storageUrl.isBlank()) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        try {
            return new UrlResource(storageUrl);
        } catch (MalformedURLException exception) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
    }

    // Upload avatar của user lên Cloudinary, ghi đè avatar cũ theo publicId cố định của user.
    public String saveAvatar(String userId, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return null;
        }

        validateAvatar(avatar);

        try {
            Map params = ObjectUtils.asMap(
                    "public_id", "avatars/" + userId + "/" + UUID.randomUUID(),
                    "resource_type", "image",
                    "invalidate", true
            );
            Map uploadResult = cloudinary.uploader().upload(avatar.getBytes(), params);
            return (String) uploadResult.get("secure_url");
        } catch (IOException exception) {
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
        }
    }

    // Xóa avatar cũ trên Cloudinary.
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }
        try {
            String publicId = extractPublicId(avatarUrl);
            if (publicId.isBlank()) {
                return;
            }
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        } catch (Exception exception) {
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
        }
    }

    // Dọn dẹp các tệp ảnh avatar cũ rác của user, chỉ giữ lại ảnh đại diện cuối cùng lưu trong DB.
    public void deleteOldAvatars(String userId, String currentAvatarUrl) {
        if (userId == null || userId.isBlank() || currentAvatarUrl == null || currentAvatarUrl.isBlank()) {
            return;
        }

        String currentPublicId = extractPublicId(currentAvatarUrl);
        if (currentPublicId.isBlank()) {
            return;
        }

        try {
            Map resourcesResult = cloudinary.api().resources(ObjectUtils.asMap(
                    "resource_type", "image",
                    "type", "upload",
                    "prefix", "avatars/" + userId,
                    "max_results", 500
            ));

            Object resources = resourcesResult.get("resources");
            if (!(resources instanceof List<?> resourceList)) {
                return;
            }

            List<String> oldPublicIds = new ArrayList<>();
            for (Object resource : resourceList) {
                if (!(resource instanceof Map<?, ?> resourceMap)) {
                    continue;
                }

                Object publicIdValue = resourceMap.get("public_id");
                if (!(publicIdValue instanceof String publicId) || publicId.equals(currentPublicId)) {
                    continue;
                }

                oldPublicIds.add(publicId);
            }

            if (!oldPublicIds.isEmpty()) {
                cloudinary.api().deleteResources(oldPublicIds, ObjectUtils.asMap(
                        "resource_type", "image",
                        "type", "upload",
                        "invalidate", true
                ));
            }
        } catch (Exception exception) {
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
        }
    }

    private void validateAvatar(MultipartFile avatar) {
        String originalFileName = avatar.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new AppException(ErrorCode.AVATAR_TYPE_INVALID);
        }
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

        if (!ALLOWED_AVATAR_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.AVATAR_TYPE_INVALID);
        }

        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new AppException(ErrorCode.AVATAR_SIZE_EXCEEDED);
        }
    }

    private String extractPublicId(String url) {
        if (url == null || url.isBlank() || !url.contains("upload/")) {
            return "";
        }

        String[] parts = url.split("upload/");
        if (parts.length < 2) {
            return "";
        }

        String splitPart = parts[1];

        if (splitPart.startsWith("v")) {
            int firstSlash = splitPart.indexOf("/");
            if (firstSlash != -1) {
                splitPart = splitPart.substring(firstSlash + 1);
            }
        }

        int lastDot = splitPart.lastIndexOf('.');
        if (lastDot != -1) {
            splitPart = splitPart.substring(0, lastDot);
        }

        return splitPart;
    }

    private String determinateResourceType(String url) {
        if (url.contains("/raw/") || url.contains(".pdf") || url.contains(".docx") || url.contains(".xlsx") || url.contains(".zip")) {
            return "raw";
        }
        if (url.contains("/video/")) {
            return "video";
        }
        return "image";
    }
}