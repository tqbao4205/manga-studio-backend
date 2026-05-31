package com.mangaflow.studio.controller.region;

import com.mangaflow.studio.dto.region.request.RegionReorderRequest;
import com.mangaflow.studio.dto.region.request.RegionRequest;
import com.mangaflow.studio.dto.region.request.RegionStatusRequest;
import com.mangaflow.studio.dto.region.response.RegionResponse;
import com.mangaflow.studio.service.region.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ── RegionController ──
 * Controller xử lý tất cả API liên quan đến Region (vùng vẽ trên page).
 * <p>
 * 📌 Base path: /api/v1 (giống PageController)
 * <p>
 * ══════════════════════════════════════════════════════════════════
 *  DANH SÁCH API:
 * ══════════════════════════════════════════════════════════════════
 *  ┌────────┬──────────────────────────────────────────┬──────────────────────┐
 *  │ Method │ Endpoint                                 │ Chức năng            │
 *  ├────────┼──────────────────────────────────────────┼──────────────────────┤
 *  │ GET    │ /pages/{pageId}/regions                  │ Danh sách regions    │
 *  │ POST   │ /pages/{pageId}/regions                  │ Tạo region mới       │
 *  │ PUT    │ /regions/{id}                            │ Cập nhật region      │
 *  │ PATCH  │ /regions/{id}/status                     │ Đổi trạng thái       │
 *  │ DELETE │ /regions/{id}                            │ Xoá region           │
 *  │ PUT    │ /pages/{pageId}/regions/reorder          │ Sắp xếp lại          │
 *  └────────┴──────────────────────────────────────────┴──────────────────────┘
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Regions", description = "Quản lý regions (vùng vẽ trên page)")
public class RegionController {

    private final RegionService regionService;

    // ════════════════════════════════════════════════════════════════
    // 1. GET REGIONS BY PAGE
    // ════════════════════════════════════════════════════════════════

    @Operation(
            summary = "Lấy danh sách regions của 1 page",
            description = "Trả về tất cả regions trong page, sắp xếp theo sortOrder tăng dần (dưới cùng lên trước)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách regions"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    @GetMapping("/pages/{pageId}/regions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RegionResponse>> getRegionsByPage(
            @Parameter(description = "ID của page", example = "1")
            @PathVariable Long pageId) {
        return ResponseEntity.ok(regionService.getRegionsByPage(pageId));
    }

    // ════════════════════════════════════════════════════════════════
    // 2. CREATE REGION
    // ════════════════════════════════════════════════════════════════

    @Operation(
            summary = "Tạo region mới trên page",
            description = "Tạo 1 vùng vẽ mới trên page. Chỉ MANAGAKA mới được dùng."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Region đã tạo"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền (chỉ MANAGAKA)")
    })
    @PostMapping("/pages/{pageId}/regions")
    @PreAuthorize("hasRole('MANAGAKA')")
    public ResponseEntity<RegionResponse> createRegion(
            @Parameter(description = "ID của page", example = "1")
            @PathVariable Long pageId,
            @Valid @RequestBody RegionRequest request) {
        RegionResponse response = regionService.createRegion(pageId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ════════════════════════════════════════════════════════════════
    // 3. UPDATE REGION
    // ════════════════════════════════════════════════════════════════

    @Operation(
            summary = "Cập nhật region",
            description = "Cập nhật thông tin region (label, type, toạ độ, kích thước, màu sắc). Chỉ MANAGAKA mới được dùng."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Region đã cập nhật"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy region"),
            @ApiResponse(responseCode = "403", description = "Không có quyền (chỉ MANAGAKA)")
    })
    @PutMapping("/regions/{id}")
    @PreAuthorize("hasRole('MANAGAKA')")
    public ResponseEntity<RegionResponse> updateRegion(
            @Parameter(description = "ID của region", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody RegionRequest request) {
        return ResponseEntity.ok(regionService.updateRegion(id, request));
    }

    // ════════════════════════════════════════════════════════════════
    // 4. UPDATE REGION STATUS
    // ════════════════════════════════════════════════════════════════

    @Operation(
            summary = "Đổi trạng thái region",
            description = "Chuyển trạng thái region: PENDING → IN_PROGRESS → COMPLETED. Chỉ MANAGAKA mới được dùng."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Region đã đổi trạng thái"),
            @ApiResponse(responseCode = "400", description = "Chuyển trạng thái không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy region"),
            @ApiResponse(responseCode = "403", description = "Không có quyền (chỉ MANAGAKA)")
    })
    @PatchMapping("/regions/{id}/status")
    @PreAuthorize("hasRole('MANAGAKA')")
    public ResponseEntity<RegionResponse> updateRegionStatus(
            @Parameter(description = "ID của region", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody RegionStatusRequest request) {
        return ResponseEntity.ok(regionService.updateRegionStatus(id, request.getStatus()));
    }

    // ════════════════════════════════════════════════════════════════
    // 5. DELETE REGION
    // ════════════════════════════════════════════════════════════════

    @Operation(
            summary = "Xoá region",
            description = "Xoá region. Chỉ xoá được khi region đang PENDING (chưa có task). Chỉ MANAGAKA mới được dùng."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã xoá thành công"),
            @ApiResponse(responseCode = "400", description = "Region đang IN_PROGRESS, không thể xoá"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy region"),
            @ApiResponse(responseCode = "403", description = "Không có quyền (chỉ MANAGAKA)")
    })
    @DeleteMapping("/regions/{id}")
    @PreAuthorize("hasRole('MANAGAKA')")
    public ResponseEntity<Void> deleteRegion(
            @Parameter(description = "ID của region cần xoá", example = "1")
            @PathVariable Long id) {
        regionService.deleteRegion(id);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════
    // 6. REORDER REGIONS
    // ════════════════════════════════════════════════════════════════

    @Operation(
            summary = "Sắp xếp lại regions (dùng cho kéo thả)",
            description = "Sắp xếp lại toàn bộ regions trên page theo thứ tự mới. Frontend gửi mảng regionIds theo thứ tự từ dưới lên trên."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách regions đã sắp xếp"),
            @ApiResponse(responseCode = "400", description = "Số lượng regionIds không khớp"),
            @ApiResponse(responseCode = "403", description = "Không có quyền (chỉ MANAGAKA)")
    })
    @PutMapping("/pages/{pageId}/regions/reorder")
    @PreAuthorize("hasRole('MANAGAKA')")
    public ResponseEntity<List<RegionResponse>> reorderRegions(
            @Parameter(description = "ID của page", example = "1")
            @PathVariable Long pageId,
            @Valid @RequestBody RegionReorderRequest request) {
        return ResponseEntity.ok(
                regionService.reorderRegions(pageId, request.getRegionIds()));
    }
}
