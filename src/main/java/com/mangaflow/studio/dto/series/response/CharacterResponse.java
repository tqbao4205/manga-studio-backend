package com.mangaflow.studio.dto.series.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ── CharacterResponse DTO ──
 * DTO trả dữ liệu character cho frontend.
 *
 * 📌 @Data + @Builder + @AllArgsConstructor (Lombok):
 *    Tự sinh getter, builder pattern, constructor.
 *
 * 📌 sketchUrls: List<String> — danh sách URL ảnh từ Cloudinary.
 *    FE dùng để hiển thị thumbnail + lightbox.
 *
 * 📌 motivation: String — HTML từ RichEditor,
 *    FE render bằng dangerouslySetInnerHTML.
 *
 * 📌 seriesId: ID của series chứa character — FE dùng để verify
 *    hoặc điều hướng nếu cần.
 */
@Data
@Builder
@AllArgsConstructor
public class CharacterResponse {

    private Long id;
    private String name;
    private String motivation;
    private List<String> sketchUrls;
    private Long seriesId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
