package com.mangaflow.studio.controller.notification;

import com.mangaflow.studio.common.security.CustomUserDetails;
import com.mangaflow.studio.dto.notification.response.NotificationResponse;
import com.mangaflow.studio.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ── NotificationController ──
 * <p>
 * Controller cung cấp REST API cho tính năng Notification.
 * Là tầng giao tiếp với Frontend — nhận HTTP request,
 * gọi NotificationService, trả về JSON response.
 * <p>
 * ══════════════════════════════════════════════════════════════════
 *  DANH SÁCH API:
 * ══════════════════════════════════════════════════════════════════
 * <p>
 *  ┌────────┬──────────────────────────────────┬──────────────────────────────┬──────────────────────┐
 *  │ Method │ Endpoint                         │ Chức năng                    │ Authentication       │
 *  ├────────┼──────────────────────────────────┼──────────────────────────────┼──────────────────────┤
 *  │ GET    │ /api/notifications               │ Lấy DS notification của user │ Bearer Token (JWT)   │
 *  │ GET    │ /api/notifications/unread-count  │ Lấy số chưa đọc (badge)      │ Bearer Token (JWT)   │
 *  │ PATCH  │ /api/notifications/{id}/read     │ Đánh dấu 1 notification đọc  │ Bearer Token (JWT)   │
 *  │ PATCH  │ /api/notifications/read-all      │ Đánh dấu tất cả đã đọc      │ Bearer Token (JWT)   │
 *  └────────┴──────────────────────────────────┴──────────────────────────────┴──────────────────────┘
 * <p>
 * ══════════════════════════════════════════════════════════════════
 *  Tóm tắt luồng xử lý 1 request:
 * ══════════════════════════════════════════════════════════════════
 * <p>
 *  [Frontend]                      (axios.get('/api/notifications', { headers: { Authorization: 'Bearer...' } }))
 *    │
 *    ▼
 *  [JwtAuthFilter]                 (kiểm tra JWT token có hợp lệ không)
 *    │
 *    ▼
 *  [NotificationController]        ← BẠN ĐANG Ở ĐÂY
 *    │  @PreAuthorize("isAuthenticated()")
 *    │  @AuthenticationPrincipal CustomUserDetails
 *    │  Gọi NotificationService
 *    ▼
 *  [NotificationService]           (xử lý logic + gọi Repository)
 *    │
 *    ▼
 *  [NotificationRepository]        (query DB qua JPA)
 *    │
 *    ▼
 *  [Database]
 *    │
 *    ▼
 *  Response JSON về Frontend
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Quản lý thông báo người dùng — lấy danh sách, đánh dấu đã đọc")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    /**
     * notificationService: Service layer chứa toàn bộ logic notification.
     * Controller chỉ làm nhiệm vụ:
     * - Nhận HTTP request
     * - Lấy thông tin user từ JWT (@AuthenticationPrincipal)
     * - Gọi service
     * - Trả về ResponseEntity
     * <p>
     * KHÔNG chứa business logic.
     */
    private final NotificationService notificationService;

    // ════════════════════════════════════════════════════════════════
    // 1. GET NOTIFICATIONS — Lấy danh sách notification
    // ════════════════════════════════════════════════════════════════

    /**
     * GET /api/notifications
     * <p>
     * 📌 Chức năng:
     *    Lấy toàn bộ notification của user đang đăng nhập,
     *    sắp xếp mới nhất lên đầu.
     * <p>
     * 📌 Frontend gọi khi:
     *    - Mở NotificationsPanel (click icon chuông)
     *    - Khởi tạo trang (để load lần đầu)
     *    - Refresh sau khi nhận WebSocket "NOTIFICATION" event
     * <p>
     * 📌 @AuthenticationPrincipal:
     *    Spring tự động inject đối tượng CustomUserDetails
     *    từ JWT token (sau khi JwtAuthFilter xác thực).
     *    Chứa thông tin user: id, email, role, ...
     *
     * @param user Thông tin user từ JWT (do Spring inject)
     * @return ResponseEntity chứa danh sách NotificationResponse
     */
    @GetMapping
    @Operation(
            summary = "Lấy danh sách thông báo",
            description = "Trả về tất cả notification của user hiện tại, sắp xếp mới nhất trước"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Thành công — trả về danh sách notification",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập — token không hợp lệ hoặc hết hạn")
    })
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // Lấy userId từ JWT token
        Long userId = user.getUserId();

        // Gọi service để lấy danh sách
        List<NotificationResponse> notifications = notificationService.getByUserId(userId);

        // Trả về 200 OK kèm danh sách JSON
        return ResponseEntity.ok(notifications);
    }

    // ════════════════════════════════════════════════════════════════
    // 2. GET UNREAD COUNT — Lấy số notification chưa đọc
    // ════════════════════════════════════════════════════════════════

    /**
     * GET /api/notifications/unread-count
     * <p>
     * 📌 Chức năng:
     *    Trả về số lượng notification chưa đọc của user.
     * <p>
     * 📌 Frontend gọi khi:
     *    - Khởi tạo Topbar (để hiển thị badge số đỏ trên icon chuông)
     *    - Sau mỗi lần đóng NotificationsPanel
     * <p>
     * 📌 Response format:
     *    { "unreadCount": 5 }
     *    — Dùng Map thay vì raw Long để dễ parse trên frontend
     *      (tránh trường hợp response là số đơn thuần khó đọc)
     *
     * @param user Thông tin user từ JWT
     * @return ResponseEntity chứa { "unreadCount": number }
     */
    @GetMapping("/unread-count")
    @Operation(
            summary = "Lấy số thông báo chưa đọc",
            description = "Trả về số lượng notification chưa đọc của user (dùng cho badge trên icon chuông)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Thành công — trả về unreadCount",
                    content = @Content(schema = @Schema(example = "{ \"unreadCount\": 5 }"))
            ),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // Lấy userId từ JWT
        Long userId = user.getUserId();

        // Gọi service đếm số chưa đọc
        long count = notificationService.getUnreadCount(userId);

        // Trả về JSON object: { "unreadCount": 5 }
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ════════════════════════════════════════════════════════════════
    // 3. MARK AS READ — Đánh dấu 1 notification đã đọc
    // ════════════════════════════════════════════════════════════════

    /**
     * PATCH /api/notifications/{id}/read
     * <p>
     * 📌 Chức năng:
     *    Đánh dấu một notification cụ thể là đã đọc.
     *    Chỉ user sở hữu notification mới có quyền gọi API này.
     * <p>
     * 📌 Frontend gọi khi:
     *    - User click vào 1 notification trong NotificationsPanel
     * <p>
     * 📌 @PathVariable Long id:
     *    ID của notification cần mark as read (từ URL path).
     *    VD: PATCH /api/notifications/5/read
     * <p>
     * 📌 Response:
     *    200 OK — không có body (Void)
     *    404 Not Found — nếu notification không tồn tại hoặc không phải của user
     *
     * @param id   ID của notification (từ URL)
     * @param user Thông tin user từ JWT
     * @return ResponseEntity 200 OK (không body) hoặc 404
     */
    @PatchMapping("/{id}/read")
    @Operation(
            summary = "Đánh dấu đã đọc",
            description = "Đánh dấu 1 notification cụ thể là đã đọc (chỉ chủ sở hữu mới có quyền)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đánh dấu thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy notification hoặc không phải của user"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // Lấy userId từ JWT
        Long userId = user.getUserId();

        // Gọi service mark as read (có kiểm tra ownership trong service)
        notificationService.markAsRead(id, userId);

        // Trả về 200 OK — không cần body
        return ResponseEntity.ok().build();
    }

    // ════════════════════════════════════════════════════════════════
    // 4. MARK ALL AS READ — Đánh dấu tất cả đã đọc
    // ════════════════════════════════════════════════════════════════

    /**
     * PATCH /api/notifications/read-all
     * <p>
     * 📌 Chức năng:
     *    Đánh dấu tất cả notification của user là đã đọc.
     *    Dùng 1 câu SQL bulk update — nhanh chóng, không cần load từng entity.
     * <p>
     * 📌 Frontend gọi khi:
     *    - User click nút "Mark all as read" trong NotificationsPanel
     * <p>
     * 📌 Response:
     *    200 OK — không có body
     * <p>
     * 📌 Không cần @PathVariable vì không có ID cụ thể —
     *    API tác động lên tất cả notification của user hiện tại.
     *
     * @param user Thông tin user từ JWT
     * @return ResponseEntity 200 OK (không body)
     */
    @PatchMapping("/read-all")
    @Operation(
            summary = "Đánh dấu tất cả đã đọc",
            description = "Đánh dấu tất cả notification của user hiện tại là đã đọc"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đánh dấu tất cả thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // Lấy userId từ JWT
        Long userId = user.getUserId();

        // Gọi service bulk update
        notificationService.markAllAsRead(userId);

        // Trả về 200 OK
        return ResponseEntity.ok().build();
    }
}
