package com.pctb.webapp.entity;

/**
 * Trang thai tham gia group cua user.
 * PENDING la cho duyet, APPROVED la da vao group.
 */
public enum JoinStatus {
    PENDING,
    APPROVED,
    REJECTED,
    LEFT,
    BANNED
}
