package com.pctb.webapp.service;

import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.util.Set;

@Service
public class DocumentTextExtractorService {
    private static final int MAX_EXTRACTED_TEXT_LENGTH = 20000;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "txt", "ppt", "pptx");

    private final AutoDetectParser parser = new AutoDetectParser();
    private final FileValidationService fileValidationService;

    public DocumentTextExtractorService(FileValidationService fileValidationService) {
        this.fileValidationService = fileValidationService;
    }

    public String extractForAi(MultipartFile file) {
        validateSupportedFileAndGetExtension(file);

        try {
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());

            parser.parse(file.getInputStream(), handler, metadata);
            String text = handler.toString();

            if (text == null || text.isBlank()) {
                throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
            }

            text = text.replaceAll("\\s+", " ").trim();
            if (text.length() > MAX_EXTRACTED_TEXT_LENGTH) {
                return text.substring(0, MAX_EXTRACTED_TEXT_LENGTH);
            }

            return text;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        }
    }

    public String validateSupportedFileAndGetExtension(MultipartFile file) {
        fileValidationService.validate(file);

        String extension = fileValidationService.getExtension(file.getOriginalFilename());
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        return extension;
    }
}
