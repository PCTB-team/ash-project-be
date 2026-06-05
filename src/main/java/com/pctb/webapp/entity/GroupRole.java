package com.pctb.webapp.entity; // Hoặc package com.pctb.webapp.entity;

public enum GroupRole {
    OWNER,   // Chủ sở hữu / Người tạo group (Có toàn quyền, xóa group)
    ADMIN,   // Quản trị viên (Có thể duyệt thành viên, kích người)
    MEMBER   // Thành viên bình thường (Chỉ xem và thảo luận)
}