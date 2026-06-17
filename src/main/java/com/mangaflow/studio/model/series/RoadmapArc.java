package com.mangaflow.studio.model.series;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ── RoadmapArc (Embeddable) ──
 *
 * 📌 @Embeddable → không phải entity riêng,
 *    được nhúng trực tiếp vào bảng phụ "series_story_roadmap"
 *    thông qua @ElementCollection trong Series.java.
 *
 * 📌 Chứa thông tin 1 Arc trong Story Roadmap:
 *     - title:   tên arc (VD: "The Awakening")
 *     - summary: tóm tắt nội dung arc
 *
 * 📌 Bảng sinh ra: series_story_roadmap
 *    ┌──────────────────────────────────────┐
 *    │ series_id  BIGINT FK → series.id     │
 *    │ title      VARCHAR                    │
 *    │ summary    VARCHAR                    │
 *    └──────────────────────────────────────┘
 *
 * 📌 Không có @Id riêng — Hibernate tự quản lý
 *    dựa trên composite key (series_id + row position).
 *
 * 📌 Dùng @Data = @Getter + @Setter + @ToString + @EqualsAndHashCode
 *    @Builder + @NoArgsConstructor + @AllArgsConstructor
 *    để tiện tạo object.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapArc {

    /**
     * title: Tên của Arc.
     * VD: "Arc 1: The Awakening", "Shadow of the Past"
     */
    private String title;

    /**
     * summary: Tóm tắt ngắn nội dung arc.
     * VD: "Nhân vật chính thức tỉnh sức mạnh"
     */
    private String summary;
}
