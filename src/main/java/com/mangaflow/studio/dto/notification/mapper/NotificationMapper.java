package com.mangaflow.studio.dto.notification.mapper;

import com.mangaflow.studio.dto.notification.response.NotificationResponse;
import com.mangaflow.studio.model.notification.Notification;
import org.mapstruct.Mapper;

/**
 * ── NotificationMapper ──
 * <p>
 * MapStruct mapper: chuyển đổi giữa entity Notification và DTO.
 * <p>
 * ══════════════════════════════════════════════════════════════════
 *  Tại sao cần Mapper?
 * ══════════════════════════════════════════════════════════════════
 *  Entity Notification chứa tất cả field trong DB.
 *  Response DTO chỉ chứa field cần thiết cho frontend.
 *  Mapper làm nhiệm vụ "copy có chọn lọc" giữa 2 class.
 * <p>
 *  Thay vì viết tay:
 *    NotificationResponse.builder()
 *        .id(notification.getId())
 *        .userId(notification.getUserId())
 *        .type(notification.getType())
 *        ...
 *        .build();
 * <p>
 *  MapStruct tự động sinh code này ở compile time.
 *  Nhờ @Mapper(componentModel = "spring"), nó trở thành Spring Bean
 *  và có thể inject vào Service.
 * <p>
 * 📌 Cấu hình:
 *    - componentModel = "spring": tích hợp với Spring DI
 *      → Có thể @Autowired / @Inject vào Service
 * <p>
 * 📌 Vì entity và DTO có tên field giống hệt nhau:
 *    - MapStruct tự động map không cần @Mapping
 *    - Chỉ cần define method toResponse() là đủ
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    /**
     * Chuyển đổi Entity → Response DTO.
     * <p>
     * MapStruct tự động map các field trùng tên:
     * - Entity.id       → Response.id
     * - Entity.userId   → Response.userId
     * - Entity.type     → Response.type
     * - Entity.title    → Response.title
     * - Entity.message  → Response.message
     * - Entity.referenceType  → Response.referenceType
     * - Entity.referenceId    → Response.referenceId
     * - Entity.isRead   → Response.isRead
     * - Entity.createdAt → Response.createdAt
     * <p>
     * ⚠️ Lưu ý: Nếu entity và DTO khác tên field, phải dùng @Mapping.
     *    Ở đây tên field giống nhau, nên không cần.
     *
     * @param notification Entity từ database
     * @return NotificationResponse DTO trả về frontend
     */
    NotificationResponse toResponse(Notification notification);
}
