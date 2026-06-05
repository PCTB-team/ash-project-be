package com.pctb.webapp.service;

import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class LocalStorageService implements StorageService {
    @Value("${app.upload.local-storage-path}")
     String storagePath;
    
    static long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    static Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("png", "jpg", "jpeg");
    static String UPLOAD_URL_PREFIX = "/uploads";

    @Value("${app.upload.dir:uploads}")
    @NonFinal
    String uploadDir;

    @Value("${server.servlet.context-path:}")
    @NonFinal
    String contextPath;
  
    // Dùng để upload file
    @Override
    public String upload(MultipartFile file, String fileName) {
        try {
            // Lấy đươờng dẫn thư mục để lưu,
            // "D:\AI-Study-Hub\ uploads"
            Path uploadDir = Path.of(storagePath).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            // Tạo đường dẫn của thư mục dùng để lưu
            Path targetPath = uploadDir.resolve(fileName).normalize();
            // Kiểm tra coi đường dẫn đích có đúng với thư mục gốc không
            // Vidu     D:\AI-Study-Hub \ uploads\java.pdf có bắt đâầu từ   //"D:\AI-Study-Hub\ uploads" không


            if (!targetPath.startsWith(uploadDir)) {
                throw new AppException(ErrorCode.UPLOAD_FAILED);
            }
            // Tạo file thư mục
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            // Thay thế file đã tồn tại
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return Path.of(storagePath).resolve(fileName).normalize().toString().replace("\\", "/");
        } catch (Exception exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String storageUrl) {
        try {
            Files.deleteIfExists(Path.of(storageUrl));
        } catch (Exception exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }
    // Dùng để tạo lại đường dẫn path mới
    @Override
    public String rename(String oldStorageUrl, String newFileName) {
        try {
            // Đường dẫn thư mục upload
            Path uploadDir = Path.of(storagePath).toAbsolutePath().normalize();
            // Đường dẫn cũ đến file
            Path oldPath = Path.of(oldStorageUrl).toAbsolutePath().normalize();
            // Đường dẫn mới khi tạo tên mới
            Path newPath = uploadDir.resolve(newFileName).normalize();

            if (!oldPath.startsWith(uploadDir) || !newPath.startsWith(uploadDir)) {
                throw new AppException(ErrorCode.UPLOAD_FAILED);
            }
            // Tạo thư mục mới nếu cần
            if (newPath.getParent() != null && !Files.exists(newPath.getParent())) {
                Files.createDirectories(newPath.getParent());
            }
            // Di chuyển đường dẫn file cũ sang file mới
            Files.move(oldPath, newPath);
            // trả về url của file mới
            return Path.of(storagePath).resolve(newFileName).normalize().toString().replace("\\", "/");
        } catch (Exception exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    @Override
    public Resource loadAsResource(String storageUrl) {
        try {
            // Lay thu muc upload goc de chan viec doc file ngoai thu muc uploads
            Path uploadDir = Path.of(storagePath).toAbsolutePath().normalize();

            // Chuyen storageUrl trong database thanh duong dan that tren may
            Path filePath = Path.of(storageUrl).toAbsolutePath().normalize();

            // Kiem tra file can tai co nam trong thu muc uploads khong
            if (!filePath.startsWith(uploadDir)) {
                throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
            }

            // Kiem tra file co ton tai that tren may khong
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
            }

            // Tao Resource de controller co the tra file ve client
            Resource resource = new UrlResource(filePath.toUri());

            // Kiem tra resource co doc duoc khong
            if (!resource.exists() || !resource.isReadable()) {
                throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
            }

            return resource;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
      
       // Lưu avatar mới xuống local storage và trả về URL để FE hiển thị lại ảnh.
    public String saveAvatar(String userId, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return null;
        }

        validateAvatar(avatar);

        String extension = getExtension(avatar.getOriginalFilename());
        Path avatarDirectory = getUploadRoot()
                .resolve("avatars")
                .resolve(userId)
                .normalize();
        String fileName = UUID.randomUUID() + "." + extension;
        Path targetPath = avatarDirectory.resolve(fileName).normalize();

        if (!targetPath.startsWith(avatarDirectory)) {
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
        }

        try {
            Files.createDirectories(avatarDirectory);
            avatar.transferTo(targetPath);
        } catch (IOException exception) {
            throw new AppException(ErrorCode.AVATAR_UPLOAD_FAILED);
        }

        return normalizeContextPath() + UPLOAD_URL_PREFIX + "/avatars/" + userId + "/" + fileName;
    }

    // Xóa avatar cũ sau khi DB đã cập nhật avatar mới thành công.
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }

        String uploadPath = stripContextPath(avatarUrl);
        if (!uploadPath.startsWith(UPLOAD_URL_PREFIX + "/")) {
            return;
        }

        Path uploadRoot = getUploadRoot();
        Path targetPath = uploadRoot
                .resolve(uploadPath.substring(UPLOAD_URL_PREFIX.length() + 1))
                .normalize();

        if (!targetPath.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException ignored) {
            // Avatar cleanup should not make a successful profile update fail.
        }
    }

    // Kiểm tra avatar theo business rule: tối đa 5MB và chỉ nhận png/jpg/jpeg.
    private void validateAvatar(MultipartFile avatar) {
        String extension = getExtension(avatar.getOriginalFilename());
        if (!ALLOWED_AVATAR_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.AVATAR_TYPE_INVALID);
        }

        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new AppException(ErrorCode.AVATAR_SIZE_EXCEEDED);
        }
    }

    // Lấy đuôi file từ tên gốc để kiểm tra định dạng avatar.
    private String getExtension(String originalFileName) {
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new AppException(ErrorCode.AVATAR_TYPE_INVALID);
        }

        return originalFileName
                .substring(originalFileName.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
    }

    // Lấy thư mục upload tuyệt đối để tránh phụ thuộc vào working directory tương đối.
    private Path getUploadRoot() {
        return Path.of(uploadDir).toAbsolutePath().normalize();
    }

    // Chuẩn hóa context path, ví dụ /api/v1, trước khi ghép vào avatarUrl.
    private String normalizeContextPath() {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }

        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }

    // Bỏ context path khỏi avatarUrl để đổi URL public thành path file local khi cần xóa.
    private String stripContextPath(String avatarUrl) {
        String normalizedContextPath = normalizeContextPath();
        if (!normalizedContextPath.isBlank() && avatarUrl.startsWith(normalizedContextPath)) {
            return avatarUrl.substring(normalizedContextPath.length());
        }

        return avatarUrl;
    }
}
