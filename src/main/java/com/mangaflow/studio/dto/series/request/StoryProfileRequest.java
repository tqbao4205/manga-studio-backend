package com.mangaflow.studio.dto.series.request;

import com.mangaflow.studio.model.series.RoadmapArc;
import lombok.Data;

import java.util.List;

/**
 * ── StoryProfileRequest ──
 *
 * 📌 DTO cho request body của PUT /api/series/{id}/story-profile.
 *    Được gửi dưới dạng JSON blob trong multipart/form-data
 *    với field name "storyProfile".
 *
 * 📌 Các field:
 *     - worldLoreContent:      HTML string từ RichEditor (World Lore)
 *     - storyRoadmap:          danh sách Arc [{title, summary}]
 *     - preservedVisualRefUrls: danh sách URL ảnh cũ cần giữ lại
 *                               (khi update, FE gửi URLs muốn giữ,
 *                                service sẽ merge với ảnh mới upload)
 *
 * 📌 Pattern giống CharacterRequest.preservedSketchUrls:
 *     FE gửi preserved URLs → BE merge + xoá URLs không còn trong danh sách
 *     khỏi Cloudinary.
 */
@Data
public class StoryProfileRequest {

    /**
     * worldLoreContent: Nội dung World Lore (HTML).
     * Lưu vào Series.worldLoreContent (TEXT column).
     */
    private String worldLoreContent;

    /**
     * storyRoadmap: Danh sách các Arc.
     * Mỗi Arc gồm title + summary.
     * Lưu vào Series.storyRoadmap (@ElementCollection).
     */
    private List<RoadmapArc> storyRoadmap;

    /**
     * preservedVisualRefUrls: Danh sách URL ảnh cũ cần giữ lại.
     * URLs trong list này sẽ được merge với ảnh mới upload.
     * URLs không còn trong list này sẽ bị xoá khỏi Cloudinary.
     */
    private List<String> preservedVisualRefUrls;
}
