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
            String resourceType = determineUploadResourceType(fileName);
            String publicId = buildUploadPublicId(fileName, resourceType);

            Map params = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", resourceType,
                    "overwrite", true
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            return (String) uploadResult.get("secure_url");
        } catch (RuntimeException exception) {
            throw mapUploadException(exception);
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
        String resourceType = determinateResourceType(storageUrl);
        String publicId = extractPublicId(storageUrl, resourceType);
        if (publicId.isBlank()) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }

        try {
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
            String resourceType = determinateResourceType(oldStorageUrl);
            String oldPublicId = extractPublicId(oldStorageUrl, resourceType);
            if (oldPublicId.isBlank()) {
                throw new AppException(ErrorCode.UPLOAD_FAILED);
            }
            String newPublicId = buildUploadPublicId(newFileName, resourceType);

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
    public String copy(String sourceStorageUrl, String newFileName) {
        if (sourceStorageUrl == null || sourceStorageUrl.isBlank()) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        try {
            String resourceType = determineUploadResourceType(newFileName);
            String publicId = buildUploadPublicId(newFileName, resourceType);
            Map params = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", resourceType,
                    "overwrite", true
            );

            Map uploadResult = cloudinary.uploader().upload(sourceStorageUrl, params);
            return (String) uploadResult.get("secure_url");
        } catch (IOException | RuntimeException exception) {
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
        } catch (RuntimeException exception) {
            throw mapAvatarUploadException(exception);
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
            String publicId = extractPublicId(avatarUrl, "image");
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

        String currentPublicId = extractPublicId(currentAvatarUrl, "image");
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
    private AppException mapUploadException(RuntimeException exception) {
        if (isCloudinaryFileTooLarge(exception)) {
            return new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        return new AppException(ErrorCode.UPLOAD_FAILED);
    }

    private AppException mapAvatarUploadException(RuntimeException exception) {
        if (isCloudinaryFileTooLarge(exception)) {
            return new AppException(ErrorCode.AVATAR_SIZE_EXCEEDED);
        }

        return new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
    }

    private boolean isCloudinaryFileTooLarge(RuntimeException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("file size too large");
    }

    private String buildUploadPublicId(String fileName, String resourceType) {
        if ("raw".equals(resourceType)) {
            return fileName;
        }

        if (fileName.contains(".")) {
            return fileName.substring(0, fileName.lastIndexOf('.'));
        }

        return fileName;
    }

    private String extractPublicId(String url, String resourceType) {
        if (url == null || url.isBlank() || !url.contains("/upload/")) {
            return "";
        }

        String[] parts = url.split("/upload/", 2);
        if (parts.length < 2) {
            return "";
        }

        String path = parts[1];
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }

        String[] segments = path.split("/");
        if (segments.length == 0) {
            return "";
        }

        int startIndex = 0;
        if (segments[startIndex].matches("v\\d+")) {
            startIndex++;
        }

        if (startIndex >= segments.length) {
            return "";
        }

        String publicId = String.join("/", java.util.Arrays.copyOfRange(segments, startIndex, segments.length));

        if (!"raw".equals(resourceType)) {
            int lastDot = publicId.lastIndexOf('.');
            if (lastDot > 0) {
                publicId = publicId.substring(0, lastDot);
            }
        }

        return publicId.trim();
    }

    private String determineUploadResourceType(String fileName) {
        String lowerCaseFileName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);

        if (lowerCaseFileName.endsWith(".mp3") || lowerCaseFileName.endsWith(".mp4")) {
            return "video";
        }

        if (lowerCaseFileName.endsWith(".pdf")
                || lowerCaseFileName.endsWith(".docx")
                || lowerCaseFileName.endsWith(".ppt")
                || lowerCaseFileName.endsWith(".pptx")
                || lowerCaseFileName.endsWith(".xls")
                || lowerCaseFileName.endsWith(".xlsx")
                || lowerCaseFileName.endsWith(".txt")
                || lowerCaseFileName.endsWith(".csv")
                || lowerCaseFileName.endsWith(".md")
                || lowerCaseFileName.endsWith(".zip")
                || lowerCaseFileName.endsWith(".rar")) {
            return "raw";
        }

        return "image";
    }

    // Xác định resource_type Cloudinary cần dùng khi destroy/rename dựa trên URL và đuôi file.
    private String determinateResourceType(String url) {
        if (url.contains("/raw/")
                || url.contains(".pdf")
                || url.contains(".docx")
                || url.contains(".ppt")
                || url.contains(".pptx")
                || url.contains(".xls")
                || url.contains(".xlsx")
                || url.contains(".txt")
                || url.contains(".csv")
                || url.contains(".md")
                || url.contains(".zip")
                || url.contains(".rar")) {
            return "raw";
        }
        if (url.contains("/video/")) {
            return "video";
        }
        return "image";
    }
}
