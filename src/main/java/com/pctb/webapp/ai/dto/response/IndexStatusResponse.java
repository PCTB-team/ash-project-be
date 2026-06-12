package com.pctb.webapp.ai.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Response trang thai index/ingest cua document hoac group file.
 */
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IndexStatusResponse {
    // Id cua Document hoac GroupFile.
    String sourceId;

    // PERSONAL_DOCUMENT hoac GROUP_FILE.
    String sourceType;

    // PENDING, PROCESSING, COMPLETED, FAILED.
    String ingestionStatus;

    // Ly do loi neu status = FAILED.
    String ingestionError;

    // Lan ingest thanh cong gan nhat.
    String lastIngestedAt;
}
