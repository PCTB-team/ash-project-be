package com.pctb.webapp.service;

import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class FileValidationService {
    private final TikaConfig tikaConfig;

    // Khởi tạo Apache Tika để đọc nội dung thật của file khi kiểm tra MIME type.
    public FileValidationService() {
        try {
            this.tikaConfig = new TikaConfig();
        } catch (TikaException | IOException exception) {
            throw new IllegalStateException("Cannot initialize Apache Tika", exception);
        }
    }

    // Tập hợp các đuôi file mà hệ thống cho phép người dùng upload.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "png", "jpg", "jpeg", "txt", "mp3", "mp4", "ppt", "pptx"
    );

    // Map từng đuôi file sang MIME type hợp lệ để ngăn client fake đuôi file.
    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES = Map.of(
            "pdf", Set.of("application/pdf"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "txt", Set.of("text/plain"),
            "mp3", Set.of("audio/mpeg"),
            "mp4", Set.of("video/mp4"),
            "ppt", Set.of("application/vnd.ms-powerpoint"),
            "pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")
    );

    @Value("${app.upload.max-file-size}")
    private long maxFileSize;

    // Kiểm tra file upload có tồn tại, không rỗng, không vượt dung lượng, đúng đuôi file và đúng MIME thật.
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_REQUIRED_UPLOAD);
        }

        if (file.getSize() > maxFileSize) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        String extension = getExtension(file.getOriginalFilename());

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String realMimeType = detectRealMimeType(file);

        if (!ALLOWED_MIME_TYPES.get(extension).contains(realMimeType)) {
            throw new AppException(ErrorCode.INVALID_MIME_TYPE);
        }

        return realMimeType;
    }

    // Lấy đuôi file từ tên file gốc và chuẩn hóa về chữ thường để so sánh.
    public String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    // Dùng Apache Tika đọc nội dung file để xác định MIME type thật, không chỉ tin dữ liệu client gửi.
    public String detectRealMimeType(MultipartFile file) {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());

        try (TikaInputStream inputStream = TikaInputStream.get(file.getInputStream())) {
            MediaType mediaType = tikaConfig.getDetector().detect(inputStream, metadata);
            return mediaType.toString();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INVALID_MIME_TYPE);
        }
    }
}
