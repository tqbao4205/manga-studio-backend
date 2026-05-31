package com.mangaflow.studio.model.region;

/**
 * ── RegionStatus ──
 * Enum định nghĩa các trạng thái của 1 region (vùng vẽ) trên page.
 * <p>
 * ════════════════════════════════════════════════════════
 *  PENDING      │  Region mới tạo, chưa có task nào
 *  IN_PROGRESS  │  Đang có task được thực hiện
 *  COMPLETED    │  Task đã hoàn thành
 * ════════════════════════════════════════════════════════
 */
public enum RegionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
