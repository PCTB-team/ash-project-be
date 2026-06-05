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

    public FileValidationService() {
        try {
            this.tikaConfig = new TikaConfig();
        } catch (TikaException | IOException exception) {
            throw new IllegalStateException("Cannot initialize Apache Tika", exception);
        }
    }

    // Tập hợp các file được hệ thống cho phép
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "png", "jpg", "jpeg", "txt", "mp3", "mp4", "ppt", "pptx"
    );

    // xác định mime Type (là kiểu dữ liệu thật của file đó) . Vidu : nếu là png thì phải có image/png để tránh fake đuôi file
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
    private long maxFileSize; // đang là 100MB

    // Hàm kiểm tra validate, truyền va title của file, multipartFile tức là các file được upload lên
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_REQUIRED_UPLOAD);
        }

        if (file.getSize() > maxFileSize) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        // sẽ lấy ra extension của file : pdf, doc, pptx, .......
        String extension = getExtension(file.getOriginalFilename());

        // Kiểm tra coi extension có trong danh sách extension cho phép không
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        // Detect MIME type from real file content instead of trusting the client.
        String realMimeType = detectRealMimeType(file);

        // Kiểm tra coi mime type đó có nằm trong danh sách cho phép không
        if (!ALLOWED_MIME_TYPES.get(extension).contains(realMimeType)) {
            throw new AppException(ErrorCode.INVALID_MIME_TYPE);
        }

        return realMimeType;
    }

    // Trả về extension :pdf
    public String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    // Kiểm tra mimeType bằng Apache tika , tức là nó sẽ mở file ra để xác định nó là gì .
    public String detectRealMimeType(MultipartFile file) {
        // Tạo ra metadata
        Metadata metadata = new Metadata();
        // Tika lấy tên file ra ,
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());

        // Đọc nội dung file , kiểm tra và trả về mime type của file
        try (TikaInputStream inputStream = TikaInputStream.get(file.getInputStream())) {
            MediaType mediaType = tikaConfig.getDetector().detect(inputStream, metadata);
            return mediaType.toString();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INVALID_MIME_TYPE);
        }
    }
}
