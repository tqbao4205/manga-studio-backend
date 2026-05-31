package com.mangaflow.studio.service.region;

import com.mangaflow.studio.common.exception.AppException;
import com.mangaflow.studio.dto.region.mapper.RegionMapper;
import com.mangaflow.studio.dto.region.request.RegionRequest;
import com.mangaflow.studio.dto.region.response.RegionResponse;
import com.mangaflow.studio.model.region.Region;
import com.mangaflow.studio.model.region.RegionStatus;
import com.mangaflow.studio.model.region.RegionType;
import com.mangaflow.studio.repository.region.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * ── RegionService ──
 * Service xử lý toàn bộ logic nghiệp vụ liên quan đến Region (vùng vẽ trên page).
 * <p>
 * 📌 @Service: Spring Bean — chứa business logic, quản lý transaction.
 * 📌 @RequiredArgsConstructor: Lombok — DI constructor cho tất cả field final.
 * 📌 @Transactional: Tất cả method đều chạy trong transaction.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RegionService {

    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;

    /**
     * Map màu mặc định cho từng loại region.
     * Dùng khi client không gửi color.
     */
    private static final Map<RegionType, String> DEFAULT_COLORS = Map.of(
            RegionType.BACKGROUND, "#4ECDC4",
            RegionType.CHARACTER, "#FF6B6B",
            RegionType.TEXT, "#FFE66D",
            RegionType.EFFECT, "#A78BFA",
            RegionType.TONE, "#6B7280",
            RegionType.OTHER, "#6B7280"
    );

    // ════════════════════════════════════════════════════════════════
    // 1. GET REGIONS BY PAGE — Lấy danh sách regions
    // ════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách regions của 1 page, sắp xếp theo sortOrder tăng dần.
     *
     * @param pageId ID của page
     * @return List<RegionResponse> danh sách regions
     */
    @Transactional(readOnly = true)
    public List<RegionResponse> getRegionsByPage(Long pageId) {
        return regionRepository.findByPageIdOrderBySortOrderAsc(pageId)
                .stream()
                .map(regionMapper::toResponse)
                .toList();
    }

    // ════════════════════════════════════════════════════════════════
    // 2. CREATE REGION — Tạo region mới
    // ════════════════════════════════════════════════════════════════

    /**
     * Tạo region mới trên 1 page.
     * <p>
     * 📌 Quy trình:
     *    1. Gán sortOrder = max hiện tại + 1 (thêm lên trên cùng)
     *    2. Gán màu mặc định nếu client không gửi
     *    3. Tạo Region entity → lưu DB
     *
     * @param pageId  ID của page
     * @param request DTO từ frontend
     * @return RegionResponse region vừa tạo
     */
    public RegionResponse createRegion(Long pageId, RegionRequest request) {
        // Lấy sortOrder lớn nhất hiện tại
        int maxSortOrder = regionRepository.findByPageIdOrderBySortOrderAsc(pageId)
                .stream()
                .mapToInt(Region::getSortOrder)
                .max()
                .orElse(-1);

        // Gán màu mặc định nếu không gửi
        String color = request.getColor();
        if (color == null || color.isBlank()) {
            color = DEFAULT_COLORS.getOrDefault(request.getRegionType(), "#6B7280");
        }

        Region region = Region.builder()
                .pageId(pageId)
                .regionType(request.getRegionType())
                .label(request.getLabel())
                .x(request.getX())
                .y(request.getY())
                .width(request.getWidth())
                .height(request.getHeight())
                .color(color)
                .sortOrder(maxSortOrder + 1)
                .status(RegionStatus.PENDING)
                .build();

        Region savedRegion = regionRepository.save(region);
        return regionMapper.toResponse(savedRegion);
    }

    // ════════════════════════════════════════════════════════════════
    // 3. UPDATE REGION — Cập nhật region
    // ════════════════════════════════════════════════════════════════

    /**
     * Cập nhật thông tin region (label, type, toạ độ, kích thước, màu sắc).
     * Chỉ cập nhật các field không null trong request.
     *
     * @param id      ID của region
     * @param request DTO từ frontend
     * @return RegionResponse region đã cập nhật
     * @throws AppException 404 — nếu không tìm thấy region
     */
    public RegionResponse updateRegion(Long id, RegionRequest request) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Region not found: " + id));

        if (request.getRegionType() != null) {
            region.setRegionType(request.getRegionType());
        }
        if (request.getLabel() != null) {
            region.setLabel(request.getLabel());
        }
        if (request.getX() != null) {
            region.setX(request.getX());
        }
        if (request.getY() != null) {
            region.setY(request.getY());
        }
        if (request.getWidth() != null) {
            region.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            region.setHeight(request.getHeight());
        }
        if (request.getColor() != null && !request.getColor().isBlank()) {
            region.setColor(request.getColor());
        }

        Region savedRegion = regionRepository.save(region);
        return regionMapper.toResponse(savedRegion);
    }

    // ════════════════════════════════════════════════════════════════
    // 4. UPDATE REGION STATUS — Đổi trạng thái region
    // ════════════════════════════════════════════════════════════════

    /**
     * Đổi trạng thái region.
     * <p>
     * 📌 Luồng hợp lệ:
     *    PENDING → IN_PROGRESS → COMPLETED
     *
     * @param id     ID của region
     * @param status Trạng thái mới
     * @return RegionResponse region đã đổi status
     * @throws AppException 404 — nếu không tìm thấy region
     * @throws AppException 400 — nếu chuyển trạng thái không hợp lệ
     */
    public RegionResponse updateRegionStatus(Long id, RegionStatus status) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Region not found: " + id));

        // Kiểm tra chuyển trạng thái hợp lệ
        RegionStatus current = region.getStatus();
        boolean valid = switch (current) {
            case PENDING -> status == RegionStatus.IN_PROGRESS;
            case IN_PROGRESS -> status == RegionStatus.COMPLETED;
            case COMPLETED -> false;
        };

        if (!valid) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Cannot change status from " + current + " to " + status);
        }

        region.setStatus(status);
        Region savedRegion = regionRepository.save(region);
        return regionMapper.toResponse(savedRegion);
    }

    // ════════════════════════════════════════════════════════════════
    // 5. DELETE REGION — Xoá region
    // ════════════════════════════════════════════════════════════════

    /**
     * Xoá region. Chỉ xoá được khi region đang PENDING.
     *
     * @param id ID của region
     * @throws AppException 404 — nếu không tìm thấy region
     * @throws AppException 400 — nếu region đang IN_PROGRESS hoặc COMPLETED
     */
    public void deleteRegion(Long id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Region not found: " + id));

        if (region.getStatus() != RegionStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Cannot delete region with status " + region.getStatus());
        }

        regionRepository.delete(region);
    }

    // ════════════════════════════════════════════════════════════════
    // 6. REORDER REGIONS — Sắp xếp lại regions
    // ════════════════════════════════════════════════════════════════

    /**
     * Sắp xếp lại thứ tự các regions trên 1 page.
     * Frontend gửi mảng regionIds theo thứ tự mới (từ dưới lên trên).
     *
     * @param pageId    ID của page
     * @param regionIds Mảng region IDs theo thứ tự mới
     * @return List<RegionResponse> danh sách regions đã sắp xếp
     * @throws AppException 400 — nếu số lượng không khớp
     */
    public List<RegionResponse> reorderRegions(Long pageId, List<Long> regionIds) {
        List<Region> regions = regionRepository.findByPageIdOrderBySortOrderAsc(pageId);

        if (regions.size() != regionIds.size()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Region count mismatch: expected " + regions.size() + " but got " + regionIds.size());
        }

        for (int i = 0; i < regionIds.size(); i++) {
            Long id = regionIds.get(i);
            Region region = regions.stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                            "Region not found: " + id));
            region.setSortOrder(i);
        }

        regionRepository.saveAll(regions);
        return regions.stream()
                .sorted(Comparator.comparingInt(Region::getSortOrder))
                .map(regionMapper::toResponse)
                .toList();
    }
}
