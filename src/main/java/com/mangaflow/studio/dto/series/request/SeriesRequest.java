package com.mangaflow.studio.dto.series.request;

import com.mangaflow.studio.model.series.Genre;
import com.mangaflow.studio.model.series.TargetDemographic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * ── SeriesRequest DTO ──
 * DTO nhận dữ liệu từ client khi tạo hoặc cập nhật series.
 *
 * 📌 @Data (Lombok): Tự sinh getter, setter cho tất cả field.
 *
 * 📌 Validation:
 *    title   → @NotBlank (bắt buộc)
 *    genres  → @NotEmpty (ít nhất 1 genre)
 *    targetDemographics → @NotEmpty (ít nhất 1 demographic)
 *    Các field còn lại → optional (nullable)
 *
 * 📌 Dùng trong:
 *    POST /api/series  → create
 *    PUT  /api/series/{id} → update
 *
 * 📌 Lưu ý với update:
 *    Client có thể gửi thiếu field → field đó sẽ không bị thay đổi
 *    (null-safe update trong SeriesService).
 */
@Data
public class SeriesRequest {

    /**
     * title: Tên series.
     * @NotBlank → không được null, không được rỗng, không được chỉ whitespace.
     * Đây là trường bắt buộc duy nhất khi tạo series.
     */
    @NotBlank(message = "Title is required")
    private String title;

    /**
     * titleJp: Tên tiếng Nhật (tuỳ chọn).
     * null → không cập nhật (khi update).
     */
    private String titleJp;

    /**
     * synopsis: Tóm tắt nội dung (tuỳ chọn).
     * Có thể là text dài.
     */
    private String synopsis;

    /**
     * genres: Danh sách thể loại (bắt buộc, ít nhất 1).
     * Client gửi JSON array → Jackson tự động parse từng phần tử sang Genre enum.
     * Vd: ["ACTION", "FANTASY"].
     * @NotEmpty → không được null, không được rỗng.
     */
    @NotEmpty(message = "At least one genre is required")
    private List<Genre> genres;

    /**
     * targetDemographics: Danh sách đối tượng độc giả (bắt buộc, ít nhất 1).
     * Vd: ["SHONEN", "SEINEN"].
     */
    @NotEmpty(message = "At least one demographic is required")
    private List<TargetDemographic> targetDemographics;

    /**
     * coverColor: Màu nền cho card (tuỳ chọn).
     * VD: "#e63946" → frontend dùng làm fallback khi chưa có ảnh bìa.
     */
    private String coverColor;

    /**
     * coverImageUrl: Đường dẫn ảnh bìa (tuỳ chọn).
     * Hiện tại chưa có upload → có thể để null.
     */
    private String coverImageUrl;
}
