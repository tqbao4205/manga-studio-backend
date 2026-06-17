package com.mangaflow.studio.service.series;

import com.mangaflow.studio.common.exception.AppException;
import com.mangaflow.studio.common.security.CustomUserDetails;
import com.mangaflow.studio.dto.series.request.StoryProfileRequest;
import com.mangaflow.studio.dto.series.response.StoryProfileResponse;
import com.mangaflow.studio.model.series.RoadmapArc;
import com.mangaflow.studio.model.series.Series;
import com.mangaflow.studio.repository.series.SeriesRepository;
import com.mangaflow.studio.service.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * ── SeriesStoryProfileService ──
 * Service chịu trách nhiệm xử lý Story Profile (World Lore + Roadmap + Visual Refs).
 *
 * 📌 @Service: Spring Bean — business logic cho story-profile.
 * 📌 @RequiredArgsConstructor: Lombok — DI constructor.
 *
 * ══════════════════════════════════════════════════
 *  Scope:
 * ══════════════════════════════════════════════════
 *  ✅ getStoryProfile()     — Query: đọc story profile của series
 *  ✅ updateStoryProfile()  — Command: cập nhật story profile (MANGAKA)
 *
 * ══════════════════════════════════════════════════
 *  Pattern tham khảo: CharacterService.create/update
 *  ══════════════════════════════════════════════════
 *  - Visual refs upload giống Character sketch upload
 *  - Merge preservedVisualRefUrls + new files giống preservedSketchUrls
 *  - Xoá ảnh cũ khỏi Cloudinary bằng deleteImageByUrl
 */
@Service
@RequiredArgsConstructor
public class SeriesStoryProfileService {

    private final SeriesRepository seriesRepository;
    private final CloudinaryService cloudinaryService;

    // ════════════════════════════════════════════════════════════
    // 1. GET STORY PROFILE — Đọc thông tin world lore + roadmap + refs
    // ════════════════════════════════════════════════════════════

    /**
     * ── getStoryProfile ──
     * Đọc story profile từ Series entity và transform sang response DTO.
     *
     * 📌 Transform mapping:
     *    Series.worldLoreContent  → StoryProfileResponse.worldLore (string)
     *    Series.storyRoadmap      → StoryProfileResponse.storyRoadmap (List<RoadmapArcDto>)
     *    Series.visualRefUrls     → StoryProfileResponse.visualReferences (List<VisualRefDto>)
     *
     * 📌 Nếu series không tồn tại → throw 404 NOT_FOUND.
     *
     * @param seriesId ID của series cần lấy story profile
     * @return StoryProfileResponse chứa worldLore, storyRoadmap, visualReferences
     */
    @Transactional(readOnly = true)
    public StoryProfileResponse getStoryProfile(Long seriesId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Series not found"));

        return toResponse(series);
    }

    // ════════════════════════════════════════════════════════════
    // 2. UPDATE STORY PROFILE — Cập nhật world lore + roadmap + refs (multipart)
    // ════════════════════════════════════════════════════════════

    /**
     * ── updateStoryProfile ──
     * Cập nhật story profile của series (chỉ MANGAKA).
     * Nhận multipart: "storyProfile" JSON blob + "files" (ảnh visual refs mới).
     *
     * 📌 Xử lý visual refs (giống CharacterService.update sketches):
     *    1. preservedVisualRefUrls != null → giữ lại các URL cũ theo danh sách FE gửi
     *    2. Xoá khỏi Cloudinary các ảnh cũ không còn trong preserved list
     *    3. Upload files mới (nếu có) — append vào cuối danh sách
     *    4. Ghi đè Series.visualRefUrls
     *
     * 📌 worldLoreContent và storyRoadmap được ghi đè trực tiếp từ request.
     *
     * @param seriesId ID của series cần cập nhật
     * @param request  StoryProfileRequest chứa worldLoreContent, storyRoadmap, preservedVisualRefUrls
     * @param files    Danh sách file ảnh mới (multipart, field name "files")
     * @param user     Current user (cần check role MANGAKA)
     * @return StoryProfileResponse sau khi cập nhật
     */
    @Transactional
    public StoryProfileResponse updateStoryProfile(Long seriesId,
                                                    StoryProfileRequest request,
                                                    List<MultipartFile> files,
                                                    CustomUserDetails user) {
        // ── Authorization: chỉ MANGAKA ──
        if (!user.getRole().equals("MANGAKA")) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Only MANGAKA can update story profile");
        }

        // ── Tìm series ──
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Series not found"));

        // ── Cập nhật worldLoreContent ──
        if (request.getWorldLoreContent() != null) {
            series.setWorldLoreContent(request.getWorldLoreContent());
        }

        // ── Cập nhật storyRoadmap ──
        if (request.getStoryRoadmap() != null) {
            List<RoadmapArc> roadmap = request.getStoryRoadmap().stream()
                    .map(arc -> RoadmapArc.builder()
                            .title(arc.getTitle())
                            .summary(arc.getSummary())
                            .build())
                    .toList();
            series.setStoryRoadmap(new ArrayList<>(roadmap));
        }

        // ── Xử lý visual refs: merge preservedUrls + new files ──
        List<String> finalRefUrls = new ArrayList<>();

        if (request.getPreservedVisualRefUrls() != null) {
            // Giữ lại các URL cũ theo danh sách FE gửi
            finalRefUrls.addAll(request.getPreservedVisualRefUrls());

            // Xoá khỏi Cloudinary các ảnh cũ không còn trong preserved list
            List<String> removedUrls = series.getVisualRefUrls().stream()
                    .filter(url -> !request.getPreservedVisualRefUrls().contains(url))
                    .toList();
            for (String url : removedUrls) {
                cloudinaryService.deleteImageByUrl(url);
            }
        }

        // Upload files mới (nếu có) — append vào cuối danh sách
        if (files != null && !files.isEmpty()) {
            int totalAfterUpload = finalRefUrls.size() + files.size();
            // Không giới hạn số lượng visual refs (khác character: max 5)
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (!file.isEmpty()) {
                    String url = cloudinaryService.uploadVisualRef(
                            file, seriesId, finalRefUrls.size());
                    finalRefUrls.add(url);
                }
            }
        }

        // Nếu có thay đổi refs → ghi đè
        if (!finalRefUrls.isEmpty()
                || (request.getPreservedVisualRefUrls() != null
                    && request.getPreservedVisualRefUrls().isEmpty())
                || (files != null && !files.isEmpty())) {
            series.setVisualRefUrls(finalRefUrls);
        }

        return toResponse(series);
    }

    // ════════════════════════════════════════════════════════════
    // PRIVATE — Map Entity → Response DTO
    // ════════════════════════════════════════════════════════════

    /**
     * ── toResponse ──
     * Transform Series entity → StoryProfileResponse DTO.
     *
     * 📌 Mapping:
     *    Series.worldLoreContent → response.worldLore
     *    Series.storyRoadmap     → response.storyRoadmap (RoadmapArcDto list)
     *    Series.visualRefUrls    → response.visualReferences (VisualRefDto list)
     */
    private StoryProfileResponse toResponse(Series series) {
        // Transform storyRoadmap: embeddable → DTO
        List<StoryProfileResponse.RoadmapArcDto> roadmapDtos = series.getStoryRoadmap().stream()
                .map(arc -> StoryProfileResponse.RoadmapArcDto.builder()
                        .title(arc.getTitle())
                        .summary(arc.getSummary())
                        .build())
                .toList();

        // Transform visualRefUrls: String list → VisualRefDto list
        List<StoryProfileResponse.VisualRefDto> refDtos = series.getVisualRefUrls().stream()
                .map(url -> StoryProfileResponse.VisualRefDto.builder()
                        .url(url)
                        .build())
                .toList();

        return StoryProfileResponse.builder()
                .worldLore(series.getWorldLoreContent())
                .storyRoadmap(roadmapDtos)
                .visualReferences(refDtos)
                .build();
    }
}
