package com.mangaflow.studio.model.region;

/**
 * ── RegionStatus ──
 * Enum định nghĩa các trạng thái của 1 region (vùng vẽ) trên page.
 * <p>
 * Region đi qua các trạng thái sau trong vòng đời của nó:
 * <p>
 * ═══════════════════════════════════════════════════════════════════════════
 *  Trạng thái     │  Ý nghĩa                                      │ Flow
 * ═══════════════════════════════════════════════════════════════════════════
 *  PENDING        │  Region mới được tạo, chưa có task nào         │ ① BẮT ĐẦU
 *                 │  Chờ MANAGAKA tạo task và gán cho ASSISTANT    │
 * ────────────────┼────────────────────────────────────────────────┼────────
 *  IN_PROGRESS    │  Có ít nhất 1 task đang được ASSISTANT thực   │ ② Khi task
 *                 │  hiện trên region này                          │    được nhận
 * ────────────────┼────────────────────────────────────────────────┼────────
 *  SUBMITTED      │  ASSISTANT đã nộp bài cho task, chờ MANAGAKA  │ ③ Khi nộp
 *                 │  duyệt                                         │    bài
 * ────────────────┼────────────────────────────────────────────────┼────────
 *  APPROVED       │  MANAGAKA đã duyệt bài nộp, kết quả đạt yêu   │ ④ Khi duyệt
 *                 │  cầu                                           │    approve
 * ────────────────┼────────────────────────────────────────────────┼────────
 *  COMPLETED      │  Region đã hoàn thành toàn bộ                  │ ⑤ KẾT THÚC
 *                 │  (có thể kết hợp nhiều task)                   │
 * ═══════════════════════════════════════════════════════════════════════════
 * <p>
 * 📌 Luồng chính: PENDING → IN_PROGRESS → SUBMITTED → APPROVED → COMPLETED
 * <p>
 * 📌 Region status được cập nhật tự động thông qua Task:
 *    - Tạo task → PENDING → IN_PROGRESS
 *    - Task nộp bài → IN_PROGRESS → SUBMITTED
 *    - Task được approve → SUBMITTED → APPROVED
 *    - Tất cả task của region done → APPROVED → COMPLETED
 */
public enum RegionStatus {
    PENDING,
    IN_PROGRESS,
    SUBMITTED,
    APPROVED,
    COMPLETED
}
