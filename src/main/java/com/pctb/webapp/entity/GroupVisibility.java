package com.pctb.webapp.entity;

public enum GroupVisibility {
    PUBLIC,  // Công khai: Ai có code cũng có thể tự bấm join vào thẳng
    PRIVATE  // Riêng tư: Phải được Owner/Admin duyệt hoặc gửi lời mời mới vào được
}
