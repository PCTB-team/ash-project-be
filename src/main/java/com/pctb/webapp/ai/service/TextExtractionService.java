package com.pctb.webapp.ai.service;

import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Doc file da upload va extract noi dung text bang Apache Tika.
 */
@Service
public class TextExtractionService {
    /**
     * Nhan Resource tu StorageService, mo input stream va parse thanh text.
     */
    public String extract(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new Tika().parseToString(inputStream);
        } catch (Exception exception) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }
}
