package com.pctb.webapp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryStorageService implements StorageService {

    Cloudinary cloudinary;

    static long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    static Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    // Upload tài liệu lên Cloudinary, bỏ đuôi file khỏi publicId vì Cloudinary tự quản lý định dạng.
    @Override
    // Gửi bytes của file lên Cloudinary và trả về secure_url để lưu vào Document.
    public String upload(MultipartFile file, String fileName) {
        try {
            String publicId = fileName;
            if (publicId.contains(".")) {
                publicId = publicId.substring(0, publicId.lastIndexOf('.'));
            }

            Map params = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "auto",
                    "overwrite", true
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            return (String) uploadResult.get("secure_url");
        } catch (IOException exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    // Xóa tài liệu khỏi Cloudinary dựa trên URL đã lưu; bỏ qua nếu URL rỗng.
    @Override
    // Xác định publicId/resourceType từ URL rồi gọi Cloudinary destroy.
    public void delete(String storageUrl) {
        if (storageUrl == null || storageUrl.isBlank()) {
            return;
        }
        try {
            String publicId = extractPublicId(storageUrl);
            String resourceType = determinateResourceType(storageUrl);

            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (IOException exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    // Đổi tên hoặc đổi đường dẫn file trên Cloudinary và trả về secure_url mới.
    @Override
    // Rename publicId trên Cloudinary để đồng bộ với tên file mới trong database.
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

    // Cloudinary trả file bằng HTTPS URL trực tiếp nên backend không cần load thành Resource như local storage.
    @Override
    // Không dùng cho Cloudinary vì client có thể truy cập file qua HTTPS URL trực tiếp.
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

    // Xóa avatar cũ trên Cloudinary; nếu xóa thất bại thì không làm hỏng luồng cập nhật profile chính.
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

    // Delete stale Cloudinary avatar assets for this user, keeping only the current DB avatar.
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

    // Kiểm tra avatar có tên file hợp lệ, đúng định dạng ảnh cho phép và không vượt 5MB.
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

    // Tách publicId từ URL Cloudinary bằng cách bỏ domain, version và đuôi mở rộng file.
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

    // Xác định resource_type Cloudinary cần dùng khi destroy/rename dựa trên URL và đuôi file.
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
