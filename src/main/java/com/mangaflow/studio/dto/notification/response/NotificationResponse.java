package com.mangaflow.studio.dto.notification.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ── NotificationResponse DTO ──
 * <p>
 * DTO trả về cho frontend khi gọi API notification.
 * Đây là phiên bản "lightweight" của entity Notification —
 * chỉ chứa các field cần thiết cho UI, không có sensitive data.
 * <p>
 * ══════════════════════════════════════════════════════════════════
 *  Luồng sử dụng:
 * ══════════════════════════════════════════════════════════════════
 *  1. API GET /api/notifications → trả về List<NotificationResponse>
 *  2. WebSocket push "NOTIFICATION" → data là NotificationResponse
 *  3. Frontend dùng response này để render NotificationsPanel
 * <p>
 * 📌 Tuân theo pattern chung của codebase:
 *    @Data + @Builder + @AllArgsConstructor
 *    để tương thích với MapStruct và Jackson serialize.
 */
@Data
@Builder
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết của một notification")
public class NotificationResponse {

    /**
     * ID duy nhất của notification.
     * Dùng để gọi API markAsRead, delete, ...
     */
    @Schema(description = "ID của notification", example = "1")
    private Long id;

    /**
     * ID của người nhận notification.
     * Frontend dùng để lọc hoặc kiểm tra quyền sở hữu.
     */
    @Schema(description = "ID người nhận", example = "1")
    private Long userId;

    /**
     * Loại thông báo.
     * Frontend dùng để render icon và xử lý navigate.
     * VD: TASK_ASSIGNED, COMMENT_ADDED, INVITATION_SENT, ...
     */
    @Schema(description = "Loại thông báo", example = "TASK_ASSIGNED")
    private String type;

    /**
     * Tiêu đề ngắn gọn của thông báo.
     * Hiển thị dòng đầu trong NotificationsPanel.
     * VD: "New submission from Tanaka"
     */
    @Schema(description = "Tiêu đề thông báo", example = "New submission from Tanaka")
    private String title;

    /**
     * Nội dung chi tiết của thông báo (có thể null).
     * Hiển thị dòng thứ hai trong NotificationsPanel.
     * VD: "Tanaka has submitted work for Background on Page 1."
     */
    @Schema(description = "Nội dung chi tiết (có thể null)", example = "Tanaka has submitted work for Background on Page 1.")
    private String message;

    /**
     * Loại entity được tham chiếu.
     * Dùng để xác định navigate đến đâu khi click.
     * VD: "TASK" → /workspace, "CHAPTER" → /workspace/{id}
     * Có thể null (notification không có link).
     */
    @Schema(description = "Loại entity tham chiếu (TASK, CHAPTER, SERIES, COMMENT)", example = "TASK")
    private String referenceType;

    /**
     * ID của entity được tham chiếu.
     * Dùng chung với referenceType để navigate.
     * VD: referenceType = "CHAPTER", referenceId = 1
     *     → navigate đến /workspace/1
     * Có thể null nếu referenceType null.
     */
    @Schema(description = "ID của entity tham chiếu", example = "610")
    private Long referenceId;

    /**
     * Trạng thái đã đọc.
     * - false: chưa đọc → highlight trong UI
     * - true:  đã đọc → hiển thị mờ
     */
    @Schema(description = "Đã đọc hay chưa", example = "false")
    private Boolean isRead;

    /**
     * Thời điểm tạo notification.
     * Frontend dùng để hiển thị relative time:
     * VD: "2 hours ago", "yesterday", "3 days ago"
     * Format ISO-8601: "2026-05-14T10:30:00"
     */
    @Schema(description = "Thời gian tạo", example = "2026-05-14T10:30:00")
    private LocalDateTime createdAt;
}
