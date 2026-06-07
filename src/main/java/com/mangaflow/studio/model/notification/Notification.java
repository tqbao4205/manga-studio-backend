package com.mangaflow.studio.model.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ── Notification Entity ──
 * <p>
 * Đại diện cho một thông báo trong hệ thống.
 * Mỗi notification gắn với một user (người nhận) và chứa thông tin
 * về loại thông báo, nội dung, và tham chiếu đến entity liên quan.
 * <p>
 * 📌 Lưu ý:
 * - userId được lưu dưới dạng Long (raw ID) thay vì @ManyToOne
 *   để tránh lazy loading và tối ưu truy vấn.
 * - Các thông báo không bao giờ bị xoá (audit log).
 * - isRead dùng Boolean wrapper (nullable) nhưng luôn có giá trị
 *   nhờ @Builder.Default và @PrePersist.
 * <p>
 * ══════════════════════════════════════════════════════════════════
 *  Các trường referenceType và referenceId:
 * ══════════════════════════════════════════════════════════════════
 *  Cặp field này dùng để "liên kết" notification đến entity gốc.
 *  Khi user click vào notification, frontend sẽ đọc referenceType
 *  để biết cần navigate đến đâu:
 * <p>
 *   referenceType = "TASK"     → navigate tới /tasks hoặc /tasks/{id}
 *   referenceType = "CHAPTER"  → navigate tới /workspace/{id}
 *   referenceType = "SERIES"   → navigate tới /series/{id}
 *   referenceType = "COMMENT"  → navigate tới workspace page
 * <p>
 *  referenceId là ID của entity gốc đó.
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /**
     * ── ID (Primary Key) ──
     * Tự động tăng bởi database (IDENTITY).
     * BIGINT trong SQL Server tương ứng với Long trong Java.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ── userId: ID của người nhận thông báo ──
     * <p>
     * Không dùng @ManyToOne vì:
     *   1. Không cần load toàn bộ entity User mỗi khi đọc notification
     *   2. Transactional read chỉ cần userId để query
     *   3. Tránh N+1 query và circular fetch
     * <p>
     * Đây là FK tới bảng users, nhưng không khai báo ràng buộc vật lý
     * ở entity (Hibernate tự quản lý nếu cần).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * ── type: Loại thông báo ──
     * <p>
     * Dùng String thay vì Enum để linh hoạt mở rộng.
     * Frontend dùng type này để render icon và xử lý navigation.
     * <p>
     * Các giá trị hiện tại:
     *   - TASK_ASSIGNED
     *   - TASK_SUBMITTED
     *   - TASK_APPROVED
     *   - TASK_REVISION_REQUIRED
     *   - COMMENT_ADDED
     *   - COMMENT_RESOLVED
     *   - CHAPTER_SUBMITTED
     *   - CHAPTER_APPROVED
     *   - CHAPTER_REJECTED
     *   - SERIES_APPROVED
     *   - SERIES_REJECTED
     *   - SERIES_CANCELLED
     *   - INVITATION_SENT
     *   - INVITATION_ACCEPTED
     *   - INVITATION_REJECTED
     *   - TANTOU_INVITATION_SENT
     *   - TANTOU_INVITATION_ACCEPTED
     *   - TANTOU_INVITATION_REJECTED
     *   - TANTOU_REVIEW_REQUIRED
     *   - TANTOU_APPROVED
     *   - TANTOU_REJECTED
     *   - RANKING_CHANGED
     *   - WARNING_ISSUED
     *   - DEADLINE_APPROACHING
     * <p>
     * Độ dài tối đa là 50 ký tự (VARCHAR(50)).
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * ── title: Tiêu đề ngắn gọn của thông báo ──
     * <p>
     * Hiển thị trực tiếp trên NotificationsPanel.
     * VD: "New submission from Tanaka", "Chapter 23 is in review"
     * <p>
     * VARCHAR(200) đủ cho hầu hết các trường hợp.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * ── message: Nội dung chi tiết của thông báo (tuỳ chọn) ──
     * <p>
     * Hiển thị bên dưới title trong NotificationsPanel (dòng thứ 2).
     * Có thể null nếu title đã đủ thông tin.
     * <p>
     * Dùng TEXT thay vì VARCHAR vì message có thể dài.
     * columnDefinition = "TEXT" đảm bảo tương thích với SQL Server.
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * ── referenceType: Loại entity được tham chiếu ──
     * <p>
     * Dùng để frontend biết navigate đến đâu khi click.
     * Các giá trị: "TASK", "CHAPTER", "SERIES", "COMMENT"
     * <p>
     * Có thể null với các thông báo không có liên kết
     * (VD: RANKING_CHANGED, DEADLINE_APPROACHING).
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * ── referenceId: ID của entity được tham chiếu ──
     * <p>
     * Dùng chung với referenceType để navigate đến trang chi tiết.
     * VD: referenceType = "CHAPTER", referenceId = 1
     *   → Frontend navigate đến /workspace/1
     * <p>
     * Null nếu referenceType null.
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * ── isRead: Trạng thái đã đọc ──
     * <p>
     - false: chưa đọc (hiển thị nổi bật trong panel)
     - true: đã đọc (mờ đi)
     * <p>
     * @Builder.Default đảm bảo Builder khởi tạo giá trị mặc định là false.
     * @PrePersist cũng set lại để phòng trường hợp dùng new thay vì Builder.
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * ── createdAt: Thời điểm tạo thông báo ──
     * <p>
     * Tự động set khi persist (không cho update sau đó).
     * Dùng LocalDateTime (Java 8+) thay vì Date.
     * Hiển thị relative time trên frontend (VD: "2 hours ago").
     */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * ── onCreate: Hook tự động set createdAt trước khi insert ──
     * <p>
     - Được JPA gọi trước khi entity được persist lần đầu.
     - Đảm bảo createdAt luôn có giá trị, kể cả khi tạo bằng new.
     - isRead cũng được đảm bảo không null.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
}
