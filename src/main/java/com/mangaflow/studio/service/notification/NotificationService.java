package com.mangaflow.studio.service.notification;

import com.mangaflow.studio.common.exception.AppException;
import com.mangaflow.studio.dto.notification.mapper.NotificationMapper;
import com.mangaflow.studio.dto.notification.response.NotificationResponse;
import com.mangaflow.studio.model.notification.Notification;
import com.mangaflow.studio.repository.notification.NotificationRepository;
import com.mangaflow.studio.service.common.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ── NotificationService ──
 * <p>
 * Service trung tâm cho toàn bộ tính năng Notification.
 * <p>
 * Đây là service mà tất cả các service khác (TaskService, CommentService,
 * SeriesAssistantService, ...) sẽ inject vào để tạo thông báo mỗi khi
 * có sự kiện nghiệp vụ xảy ra.
 * <p>
 * ══════════════════════════════════════════════════════════════════
 *  Luồng xử lý khi có sự kiện (VD: giao việc cho Assistant):
 * ══════════════════════════════════════════════════════════════════
 * <p>
 *  TaskService.createTask()
 *    └→ notificationService.createAndSend(...)     ← NHÀ: Bước này
 *         ├─ 1. Tạo entity Notification            ← persist vào DB
 *         ├─ 2. Map sang NotificationResponse      ← convert DTO
 *         └─ 3. webSocketService.sendToUser(...)   ← push realtime
 *              └→ Frontend nhận "NOTIFICATION"
 *                   ├─ notificationStore.addNotification()  ← cập nhật list
 *                   └─ uiStore.addToast()                   ← show toast
 * <p>
 * 📌 Lưu ý: Service này KHÔNG thay thế webSocketService.sendToUser()
 *    hiện tại ở các service khác. Các service giữ nguyên WS event cũ
 *    (VD: "INVITATION_SENT") để trigger refetch, và GỌI THÊM
 *    createAndSend() để persist + push "NOTIFICATION" event riêng.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    // ════════════════════════════════════════════════════════════════
    // DI — Dependency Injection
    // ════════════════════════════════════════════════════════════════

    /**
     * Repository để CRUD notification trong DB.
     */
    private final NotificationRepository notificationRepository;

    /**
     * MapStruct mapper: Entity → DTO.
     */
    private final NotificationMapper notificationMapper;

    /**
     * WebSocket service: push realtime message đến frontend.
     * Dùng để gửi event "NOTIFICATION" ngay sau khi persist.
     */
    private final WebSocketService webSocketService;

    // ════════════════════════════════════════════════════════════════
    //  1. CREATE_AND_SEND — Tạo notification + push WebSocket
    // ════════════════════════════════════════════════════════════════

    /**
     * Tạo một notification mới, persist vào DB, và push realtime
     * đến frontend qua WebSocket với event type "NOTIFICATION".
     * <p>
     * Đây là method quan trọng nhất — được gọi từ các service khác
     * mỗi khi có sự kiện cần thông báo cho user.
     * <p>
     * ══════════════════════════════════════════════════════════════
     *  Flow chi tiết:
     * ══════════════════════════════════════════════════════════════
     *  1. Build entity Notification từ các tham số đầu vào
     *  2. Save entity xuống DB (notificationRepository.save)
     *  3. Map entity → NotificationResponse DTO
     *  4. Push WebSocket event "NOTIFICATION" kèm DTO đến user
     *  5. Trả về DTO (nếu caller cần dùng tiếp)
     * <p>
     * ══════════════════════════════════════════════════════════════
     *  Cách gọi từ các service khác (VD: TaskService):
     * ══════════════════════════════════════════════════════════════
     *  notificationService.createAndSend(
     *      assistantId,                          // userId: ai nhận?
     *      "TASK_ASSIGNED",                      // type: loại gì?
     *      "New task: " + taskRequest.getTitle(),// title: tiêu đề
     *      "You have been assigned...",          // message: nội dung
     *      "TASK",                               // referenceType: navigate đến đâu?
     *      task.getId()                          // referenceId: ID entity
     *  );
     * <p>
     * ══════════════════════════════════════════════════════════════
     *  Frontend nhận được WebSocket message:
     * ══════════════════════════════════════════════════════════════
     *  {
     *    "type": "NOTIFICATION",
     *    "data": {
     *      "id": 42,
     *      "userId": 2,
     *      "type": "TASK_ASSIGNED",
     *      "title": "New task: Castle Background",
     *      "message": "You have been assigned...",
     *      "referenceType": "TASK",
     *      "referenceId": 610,
     *      "isRead": false,
     *      "createdAt": "2026-06-07T10:30:00"
     *    }
     *  }
     *
     * @param userId        ID của người nhận (bắt buộc)
     * @param type          Loại thông báo (VD: "TASK_ASSIGNED")
     * @param title         Tiêu đề ngắn (VD: "New task: Castle Background")
     * @param message       Nội dung chi tiết (có thể null)
     * @param referenceType Loại entity tham chiếu (VD: "TASK", null nếu không có)
     * @param referenceId   ID của entity tham chiếu (null nếu không có)
     * @return NotificationResponse — DTO đã persist (để caller dùng nếu cần)
     */
    public NotificationResponse createAndSend(
            Long userId,
            String type,
            String title,
            String message,
            String referenceType,
            Long referenceId
    ) {
        // ─── 1. Build entity ───
        // Dùng Builder pattern (Lombok @Builder trên entity)
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead(false)      // mặc định chưa đọc
                .build();

        // ─── 2. Persist xuống DB ───
        // @PrePersist sẽ tự động set createdAt và isRead
        notification = notificationRepository.save(notification);

        // ─── 3. Map Entity → DTO ───
        NotificationResponse response = notificationMapper.toResponse(notification);

        // ─── 4. Push WebSocket realtime ───
        // Event type "NOTIFICATION" — frontend xử lý trong handleWebSocketMessage
        // Dùng chính NotificationResponse làm data (chứa tất cả thông tin cần)
        webSocketService.sendToUser(userId, "NOTIFICATION", response);

        // ─── 5. Return DTO ───
        return response;
    }

    // ════════════════════════════════════════════════════════════════
    //  2. GET_BY_USER_ID — Lấy danh sách notification cho NotificationsPanel
    // ════════════════════════════════════════════════════════════════

    /**
     * Lấy tất cả notification của một user, sắp xếp mới nhất lên đầu.
     * <p>
     * Dùng để hiển thị NotificationsPanel trên frontend.
     * Kết quả đã sẵn sàng để trả về qua API.
     * <p>
     * ⚠️ Không phân trang vì số lượng notification mỗi user thường
     * không quá lớn (< 200). Nếu cần, có thể thêm Pageable sau.
     *
     * @param userId ID của user
     * @return List<NotificationResponse> — danh sách đã sắp xếp
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByUserId(Long userId) {
        // Gọi repository query derived
        List<Notification> notifications = notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId);

        // Map từng entity sang DTO bằng MapStruct
        return notifications.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    //  3. GET_UNREAD_COUNT — Lấy số lượng chưa đọc cho badge
    // ════════════════════════════════════════════════════════════════

    /**
     * Đếm số notification chưa đọc của user.
     * <p>
     * Frontend gọi API này khi mount để lấy badge số đỏ trên icon chuông.
     * Cũng được cập nhật realtime qua WebSocket mỗi khi có notification mới.
     *
     * @param userId ID của user
     * @return Số lượng notification chưa đọc (0 nếu không có)
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // ════════════════════════════════════════════════════════════════
    //  4. MARK_AS_READ — Đánh dấu 1 notification đã đọc
    // ════════════════════════════════════════════════════════════════

    /**
     * Đánh dấu một notification cụ thể là đã đọc.
     * <p>
     * Kiểm tra ownership: chỉ user sở hữu mới có quyền markAsRead.
     * Nếu notification không tồn tại hoặc không phải của user → throw 404.
     *
     * @param id     ID của notification cần mark as read
     * @param userId ID của user (để kiểm tra quyền sở hữu)
     * @throws AppException (404) nếu không tìm thấy hoặc không phải của user
     */
    public void markAsRead(Long id, Long userId) {
        // Kiểm tra notification có tồn tại và thuộc về user không
        Notification notification = notificationRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found"
                ));

        // Nếu chưa đọc → set isRead = true
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            // Save vì đang trong transaction (@Transactional)
            // Spring Data JPA tự động flush khi commit
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  5. MARK_ALL_AS_READ — Đánh dấu tất cả đã đọc
    // ════════════════════════════════════════════════════════════════

    /**
     * Đánh dấu tất cả notification của user là đã đọc.
     * <p>
     * Dùng bulk update (JPQL @Modifying) thay vì for-loop + save()
     * để tối ưu performance — chỉ mất 1 câu SQL.
     *
     * @param userId ID của user
     * @return Số notification được update (có thể dùng để log)
     */
    public int markAllAsRead(Long userId) {
        // Gọi bulk update query từ repository
        return notificationRepository.markAllAsReadByUserId(userId);
    }
}
