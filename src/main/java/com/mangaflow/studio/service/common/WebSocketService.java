package com.mangaflow.studio.service.common;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ── WebSocketService ──
 * Service utility để push real-time message từ backend đến frontend qua WebSocket.
 * <p>
 * 📌 @Service: Spring Bean — có thể inject vào bất kỳ Service nào khác.
 * 📌 @RequiredArgsConstructor: Lombok — tự động inject SimpMessagingTemplate.
 * <p>
 * ══════════════════════════════════════════════════════════════════
 *  Cách dùng:
 * ══════════════════════════════════════════════════════════════════
 *  // Trong bất kỳ Service nào, chỉ cần 1 dòng:
 *  webSocketService.sendToUser(assistantId, "TASK_ASSIGNED", taskResponse);
 * <p>
 *  // Frontend nhận được:
 *  {
 *    "type": "TASK_ASSIGNED",
 *    "data": { "id": 1, "title": "Vẽ nhân vật chính", ... }
 *  }
 * <p>
 * 📌 SimpMessagingTemplate:
 *    - Là class của Spring WebSocket — dùng để gửi message đến STOMP topic.
 *    - Được Spring tự động tạo khi có @EnableWebSocketMessageBroker.
 *    - convertAndSend(topic, payload) → push message đến tất cả subscriber của topic đó.
 */
@Service
@RequiredArgsConstructor
public class WebSocketService {

    /**
     * messagingTemplate: Công cụ gửi message qua STOMP.
     * Spring tự động inject bean này nhờ @EnableWebSocketMessageBroker.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * sendToUser: Gửi real-time message đến 1 user cụ thể.
     * <p>
     * 📌 Cách hoạt động:
     *    1. Frontend subscribe vào topic /topic/user/{userId}
     *       (VD: user id = 5 → subscribe /topic/user/5)
     *    2. Backend gọi sendToUser(5, "INVITATION_SENT", data)
     *    3. Message được push đến topic /topic/user/5
     *    4. Tất cả frontend của user 5 đang subscribe topic đó đều nhận được
     * <p>
     * 📌 Format message:
     *    {
     *      "type": "INVITATION_SENT",     // loại sự kiện
     *      "data": { ... }                // nội dung (bất kỳ DTO nào)
     *    }
     * <p>
     * 📌 type là String để frontend dùng switch-case xử lý:
     *    - "INVITATION_SENT"        → ASSISTANT có lời mời mới
     *    - "INVITATION_ACCEPTED"   → MANGAKA biết assistant đã đồng ý
     *    - "INVITATION_REJECTED"   → MANGAKA biết assistant đã từ chối
     *    - "TASK_ASSIGNED"         → ASSISTANT có task mới
     *    - (Có thể mở rộng thêm sau này)
     *
     * @param userId ID của user cần nhận thông báo (frontend đã subscribe topic của user này)
     * @param type   Loại sự kiện (String) — frontend dùng để phân biệt
     * @param data   Dữ liệu gửi kèm (có thể là bất kỳ DTO/Response nào)
     */
    public void sendToUser(Long userId, String type, Object data) {
        // Tạo payload JSON: { "type": "...", "data": ... }
        Map<String, Object> payload = Map.of(
                "type", type,
                "data", data
        );

        // Push message đến topic riêng của user
        // VD: userId = 5 → topic = /topic/user/5
        messagingTemplate.convertAndSend("/topic/user/" + userId, payload);
    }
}
