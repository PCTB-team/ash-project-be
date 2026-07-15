package com.pctb.webapp.event;

import java.util.List;

/**
 * Sự kiện nội bộ được tạo sau khi service hoàn tất thao tác xóa nhóm trong DB.
 * Danh sách storageUrls được giữ lại vì các bản ghi group_file sẽ không còn sau khi commit.
 */
public record GroupDeletedEvent(
        String groupId,
        String groupName,
        String deletedAt,
        List<String> storageUrls
) {
}
