package com.pctb.webapp.ai.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Response nguon trich dan ma AI da dung de tra loi.
 * FE co the hien preview va link ve document/group file dua tren id.
 */
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiCitationResponse {
    // Id chunk duoc retrieval.
    String chunkId;

    // PERSONAL_DOCUMENT hoac GROUP_FILE.
    String sourceType;

    // Document ca nhan neu citation den tu document.
    String documentId;

    // Group file neu citation den tu group.
    String groupFileId;

    // Doan text ngan de user thay AI lay nguon tu dau.
    String preview;

    // So trang neu parser lay duoc, MVP tam null.
    Integer pageNumber;
}
