package com.mangaflow.studio.controller.dashboard;

import com.mangaflow.studio.common.security.CustomUserDetails;
import com.mangaflow.studio.dto.dashboard.response.*;
import com.mangaflow.studio.service.dashboard.MangakaDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/mangaka")
@PreAuthorize("hasRole('MANGAKA')")
@RequiredArgsConstructor
@Tag(name = "Mangaka Dashboard", description = "API thống kê cá nhân cho Mangaka")
public class MangakaDashboardController {

    private final MangakaDashboardService mangakaDashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Tổng quan cá nhân")
    public ResponseEntity<MangakaOverviewResponse> getOverview(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(mangakaDashboardService.overview(user.getUserId()));
    }

    @GetMapping("/my-series")
    @Operation(summary = "Danh sách series kèm thông số thống kê")
    public ResponseEntity<List<MySeriesStatResponse>> getMySeries(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(mangakaDashboardService.getMySeries(user.getUserId()));
    }

    @GetMapping("/my-series/{seriesId}/chapter-progress")
    @Operation(summary = "Tiến độ chapter của một series")
    public ResponseEntity<ChapterProgressResponse> getChapterProgress(
            @PathVariable Long seriesId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                mangakaDashboardService.getChapterProgress(seriesId, user.getUserId()));
    }

    @GetMapping("/my-series/{seriesId}/team")
    @Operation(summary = "Thông tin team assistant")
    public ResponseEntity<TeamStatResponse> getTeamStat(
            @PathVariable Long seriesId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                mangakaDashboardService.getTeamStat(seriesId, user.getUserId()));
    }
}
