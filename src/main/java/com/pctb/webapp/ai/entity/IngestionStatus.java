package com.pctb.webapp.ai.entity;

/**
 * Trang thai bien file thanh text chunks de AI co the tim kiem.
 */
public enum IngestionStatus {
    // File moi upload, chua bat dau doc noi dung.
    PENDING,

    // Backend dang extract text va cat chunk.
    PROCESSING,

    // File da duoc index thanh chunks thanh cong.
    COMPLETED,

    // Qua trinh index bi loi, xem ingestionError de biet ly do.
    FAILED
}
