package com.pctb.webapp.event;

import com.pctb.webapp.dto.response.GroupEventResponse;
import com.pctb.webapp.service.StorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupDeletedEventListener {
    StorageService storageService;

    SimpMessagingTemplate messagingTemplate;

    /**
     * Chỉ chạy sau khi transaction DB đã commit thành công.
     * Hàm gửi sự kiện realtime cho FE rồi dọn các file vật lý khỏi storage.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupDeleted(GroupDeletedEvent event) {
        publishGroupDeletedEvent(event);
        deleteStoredFiles(event);
    }

    private void publishGroupDeletedEvent(GroupDeletedEvent event) {
        try {
            // Tạo payload riêng cho kênh sự kiện, không làm thay đổi payload chat hiện tại.
            GroupEventResponse response = GroupEventResponse.builder()
                    .type("GROUP_DELETED")
                    .groupId(event.groupId())
                    .groupName(event.groupName())
                    .occurredAt(event.deletedAt())
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/groups/" + event.groupId() + "/events",
                    response
            );
        } catch (RuntimeException exception) {
            // DB đã xóa thành công nên lỗi WebSocket chỉ được ghi log, không trả lỗi giả cho FE.
            log.error("Không thể gửi sự kiện xóa nhóm {} qua WebSocket", event.groupId(), exception);
        }
    }

    private void deleteStoredFiles(GroupDeletedEvent event) {
        // Xóa từng file riêng để một file lỗi không ngăn các file còn lại được dọn.
        for (String storageUrl : event.storageUrls()) {
            try {
                storageService.delete(storageUrl);
            } catch (RuntimeException exception) {
                // DB đã xóa thành công, URL lỗi được ghi log để quản trị viên dọn lại sau.
                log.error("Không thể xóa file storage của nhóm {}: {}", event.groupId(), storageUrl, exception);
            }
        }
    }
}
