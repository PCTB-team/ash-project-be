package com.pctb.webapp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryStorageService implements StorageService {

    Cloudinary cloudinary;

    static long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    static Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    // 1. Upload tài liệu lên Cloudinary
    @Override
    public String upload(MultipartFile file, String fileName) {
        try {
            // Loại bỏ phần mở rộng nếu có trong fileName vì Cloudinary tự quản lý extension
            String publicId = fileName;
            if (publicId.contains(".")) {
                publicId = publicId.substring(0, publicId.lastIndexOf('.'));
            }

            Map params = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "auto", // Tự động nhận diện pdf, docx, png, mp4...
                    "overwrite", true
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            // Trả về URL HTTPS trực tiếp từ Cloudinary đám mây
            return (String) uploadResult.get("secure_url");
        } catch (IOException exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    // 2. Xóa tài liệu khỏi Cloudinary dựa vào URL
    @Override
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

    // 3. Đổi tên/Đổi đường dẫn file trên Cloudinary
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

    // 4. Hàm cũ của Local Storage không cần dùng cho Cloud đám mây
    @Override
    public Resource loadAsResource(String storageUrl) {
        // Đối với Cloudinary, Frontend sử dụng trực tiếp Link HTTPS để Preview/Download
        return null;
    }

    // 5. Lưu Avatar người dùng lên Cloudinary
    public String saveAvatar(String userId, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return null;
        }

        validateAvatar(avatar);

        try {
            Map params = ObjectUtils.asMap(
                    "public_id", "avatars/" + userId,
                    "resource_type", "image",
                    "overwrite", true
            );
            Map uploadResult = cloudinary.uploader().upload(avatar.getBytes(), params);
            return (String) uploadResult.get("secure_url");
        } catch (IOException exception) {
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
        }
    }

    // 6. Xóa Avatar cũ trên Cloudinary
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }
        try {
            String publicId = extractPublicId(avatarUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException ignored) {
            // Không làm sập luồng chính nếu dọn dẹp thất bại
        }
    }

    // --- HÀM TRỢ GIÚP (HELPER METHODS) ---

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
            return ""; // Hoặc log cảnh báo: "URL không hợp lệ"
        }

        // Tách lấy phần sau "upload/"
        String[] parts = url.split("upload/");
        if (parts.length < 2) return "";

        String splitPart = parts[1];

        // Bỏ qua version (ví dụ: v1234567/...)
        if (splitPart.startsWith("v")) {
            int firstSlash = splitPart.indexOf("/");
            if (firstSlash != -1) {
                splitPart = splitPart.substring(firstSlash + 1);
            }
        }

        // Bỏ đuôi mở rộng file (.pdf, .png...)
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
