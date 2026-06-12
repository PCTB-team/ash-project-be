package com.pctb.webapp.ai.entity;

/**
 * Vai tro cua tung message trong lich su chat.
 */
public enum MessageRole {
    // Tin nhan nguoi dung gui len.
    USER,

    // Tin nhan backend/model AI tra ve.
    ASSISTANT,

    // Tin nhan huong dan he thong, thuong khong hien cho user.
    SYSTEM
}
