package com.pctb.webapp.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Dữ liệu trả về cho FE sau khi nhóm đã được xóa thành công.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeleteGroupResponse {
    // ID của nhóm vừa bị xóa để FE loại nhóm khỏi danh sách hiện tại.
    String groupId;

    // Tên nhóm dùng để FE hiển thị thông báo thành công.
    String groupName;

    // Thời điểm nhóm bị xóa.
    String deletedAt;
}
