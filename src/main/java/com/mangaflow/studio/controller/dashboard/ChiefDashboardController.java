package com.mangaflow.studio.controller.dashboard;

import com.mangaflow.studio.dto.dashboard.response.*;
import com.mangaflow.studio.model.series.Series;
import com.mangaflow.studio.service.dashboard.ChiefDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/chief")
@PreAuthorize("hasAnyRole('CHIEF_EDITOR', 'EDITORIAL_BOARD')")
@RequiredArgsConstructor
@Tag(name = "Chief Dashboard", description = "API thống kê tổng quan cho Chief Editor & Editorial Board")
public class ChiefDashboardController {

    private final ChiefDashboardService chiefDashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Tổng quan nền tảng")
    public ResponseEntity<ChiefOverviewResponse> getOverview() {
        return ResponseEntity.ok(chiefDashboardService.overview());
    }

    @GetMapping("/series-by-status")
    @Operation(summary = "Phân bố series theo trạng thái")
    public ResponseEntity<List<SeriesStatusSummary>> getSeriesByStatus() {
        return ResponseEntity.ok(chiefDashboardService.getSeriesByStatus());
    }

    @GetMapping("/tier-distribution")
    @Operation(summary = "Phân bố tier S/A/B/C/D")
    public ResponseEntity<TierDistributionResponse> getTierDistribution() {
        return ResponseEntity.ok(chiefDashboardService.getTierDistribution());
    }

    @GetMapping("/top-series")
    @Operation(summary = "Top N series theo composite score")
    public ResponseEntity<List<TopSeriesResponse>> getTopSeries(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(chiefDashboardService.getTopSeries(limit, month));
    }

    @GetMapping("/at-risk-series")
    @Operation(summary = "Danh sách series đang bị cảnh báo")
    public ResponseEntity<List<Series>> getAtRiskSeries() {
        return ResponseEntity.ok(chiefDashboardService.getAtRiskSeries());
    }

    @GetMapping("/pending-actions")
    @Operation(summary = "Công việc cần xử lý")
    public ResponseEntity<PendingActionsResponse> getPendingActions() {
        return ResponseEntity.ok(chiefDashboardService.getPendingActions());
    }
}
