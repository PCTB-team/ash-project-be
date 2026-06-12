package com.pctb.webapp.ai.entity;

/**
 * Xac dinh pham vi tai lieu ma mot chat session duoc phep hoi.
 */
public enum ChatScopeType {
    // Chat tren mot document ca nhan cu the.
    PERSONAL_DOCUMENT,

    // Chat tren tat ca document trong mot folder ca nhan.
    PERSONAL_FOLDER,

    // Chat tren toan bo thu vien document ca nhan cua user.
    PERSONAL_LIBRARY,

    // Chat tren cac document active trong mot study group.
    GROUP_DOCUMENTS
}
