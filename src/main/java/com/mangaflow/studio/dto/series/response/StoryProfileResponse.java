package com.mangaflow.studio.dto.series.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ── StoryProfileResponse ──
 *
 * 📌 DTO cho response của GET /api/series/{id}/story-profile.
 *
 * 📌 Format trả về FE:
 *     {
 *       "worldLore":        "<h2>Thế giới phép thuật</h2><p>...</p>",
 *       "storyRoadmap":     [{ "title": "...", "summary": "..." }],
 *       "visualReferences": [{ "url": "https://..." }]
 *     }
 *
 * 📌 Service transform từ Series entity sang format này:
 *     - worldLoreContent → worldLore (string đơn, HTML từ RichEditor)
 *     - storyRoadmap     → storyRoadmap (giữ nguyên)
 *     - visualRefUrls    → visualReferences[].url
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryProfileResponse {

    private String worldLore;
    private List<RoadmapArcDto> storyRoadmap;
    private List<VisualRefDto> visualReferences;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadmapArcDto {
        private String title;
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualRefDto {
        private String url;
    }
}
