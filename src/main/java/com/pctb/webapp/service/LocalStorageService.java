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

@Service
public class LocalStorageService implements StorageService {
    @Value("${app.upload.local-storage-path}")
    private String storagePath;

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
    }
}
