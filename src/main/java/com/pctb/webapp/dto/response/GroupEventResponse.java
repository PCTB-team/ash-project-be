package com.pctb.webapp.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Dữ liệu sự kiện nhóm được gửi realtime cho FE qua WebSocket.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupEventResponse {
    // Loại sự kiện để FE chọn cách xử lý, ví dụ GROUP_DELETED.
    String type;

    // Nhóm xảy ra sự kiện.
    String groupId;

    // Tên nhóm giúp FE hiển thị nội dung thông báo.
    String groupName;

    // Thời điểm sự kiện xảy ra.
    String occurredAt;
}
