package com.mangaflow.studio.repository.series;

import com.mangaflow.studio.model.series.Series;
import com.mangaflow.studio.model.series.SeriesStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ── SeriesRepository ──
 * Repository cho entity Series — tầng giao tiếp với database.
 *
 * 📌 extends JpaRepository<Series, Long>:
 *    Cung cấp sẵn các method CRUD: findAll(), findById(), save(), delete(),...
 *
 * 📌 extends JpaSpecificationExecutor<Series>:
 *    Cho phép build query động bằng Specification (xem SeriesService.getAll()).
 *    Thay thế cho việc phải viết nhiều method findByXxx trong interface.
 *
 * 📌 Chỉ giữ lại 2 method đặc thù:
 *    - findByIdAndMangakaId: dùng để kiểm tra ownership
 *    - countByMangakaId: đếm số series của 1 mangaka
 *
 * 📌 Các query findByStatus, findByTitleContaining...:
 *    Không cần khai báo ở đây — dùng Specification trong Service.
 */
@Repository
public interface SeriesRepository extends JpaRepository<Series, Long>,
                                          JpaSpecificationExecutor<Series> {

    /**
     * Tìm series theo id + mangakaId.
     * Dùng để kiểm tra ownership trước khi update/delete.
     *
     * Nếu không tìm thấy → series không tồn tại hoặc không phải của user này
     * → throw 403 Forbidden.
     *
     * @param id id của series
     * @param mangakaId id của mangaka (lấy từ token)
     * @return Optional<Series> — empty nếu không phải chủ sở hữu
     */
    Optional<Series> findByIdAndMangakaId(Long id, Long mangakaId);

    /**
     * Đếm số series của một mangaka.
     *
     * @param mangakaId id của mangaka
     * @return số lượng series
     */
    long countByMangakaId(Long mangakaId);

    /**
     * Tìm tất cả series có status nằm trong danh sách cho trước.
     * Dùng để lấy danh sách series đang phát hành (ONGOING + AT_RISK)
     * khi export file Excel form chấm điểm hàng tháng.
     *
     * @param statuses Danh sách trạng thái cần lọc
     * @return Danh sách series khớp
     */
    List<Series> findByStatusIn(List<SeriesStatus> statuses);

    // ═══════════════════════════════════════════════════════════
    //  DASHBOARD STATISTICS — Thêm cho Series Statistics Feature
    // ═══════════════════════════════════════════════════════════

    /**
     * Đếm series theo một trạng thái cụ thể.
     * Dùng trong ChiefDashboardService: đếm ONGOING, AT_RISK, COMPLETED...
     */
    long countByStatus(SeriesStatus status);

    /**
     * Đếm series được tạo trong khoảng thời gian.
     * Dùng trong ChiefDashboardService: đếm series mới trong tháng.
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Tìm tất cả series của một mangaka.
     * Dùng trong MangakaDashboardService: lấy danh sách series của user.
     */
    List<Series> findByMangakaId(Long mangakaId);

    /**
     * Đếm series của mangaka theo trạng thái.
     * Dùng trong MangakaDashboardService: đếm series ONGOING của user.
     */
    long countByMangakaIdAndStatus(Long mangakaId, SeriesStatus status);

    /**
     * Group series theo trạng thái — đếm số lượng mỗi status.
     * Dùng trong ChiefDashboardService: biểu đồ phân bố series theo trạng thái.
     * Trả về List<Object[]> với mỗi phần tử [status (String), count (Long)].
     */
    @Query("SELECT s.status, COUNT(s) FROM Series s GROUP BY s.status")
    List<Object[]> countByStatusGrouped();

    /**
     * Group series theo currentTier — đếm số lượng mỗi tier.
     * Dùng trong ChiefDashboardService: biểu đồ phân bố tier S/A/B/C/D.
     * Chỉ đếm các series có currentTier IS NOT NULL.
     * Trả về List<Object[]> với mỗi phần tử [tier (String), count (Long)].
     */
    @Query("SELECT s.currentTier, COUNT(s) FROM Series s " +
           "WHERE s.currentTier IS NOT NULL GROUP BY s.currentTier")
    List<Object[]> countByTierGrouped();
}
