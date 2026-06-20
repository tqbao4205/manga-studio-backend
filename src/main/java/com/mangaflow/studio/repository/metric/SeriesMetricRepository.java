package com.mangaflow.studio.repository.metric;

import com.mangaflow.studio.model.metric.SeriesMetric;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 🗄️ SeriesMetricRepository - Lớp giao tiếp với database cho bảng series_metrics.
 * <p>
 * Đây là tầng Repository (DAO), có nhiệm vụ truy vấn dữ liệu từ database.
 * JpaRepository đã cung cấp sẵn các method CRUD cơ bản (save, findAll, findById, delete...).
 * Các method custom dưới đây được Spring Data JPA tự động sinh code dựa trên tên method.
 */
@Repository
public interface SeriesMetricRepository extends JpaRepository<SeriesMetric, Long> {

    /**
     * Tìm metric của 1 series trong 1 tháng cụ thể.
     * Dùng để kiểm tra: nếu đã có thì cập nhật (update), chưa có thì tạo mới (insert).
     *
     * @param seriesId ID của series
     * @param month    Tháng định dạng "YYYY-MM"
     * @return Optional<SeriesMetric> - Có thể có hoặc không
     */
    Optional<SeriesMetric> findBySeriesIdAndMonth(Long seriesId, String month);

    /**
     * Lấy tất cả metrics của 1 series, sắp xếp theo tháng MỚI NHẤT trước.
     * Dùng cho màn hình lịch sử metric của từng series.
     *
     * @param seriesId ID của series
     * @return Danh sách metrics đã sắp xếp
     */
    List<SeriesMetric> findBySeriesIdOrderByMonthDesc(Long seriesId);

    /**
     * Lấy tất cả metrics của 1 tháng cụ thể (tất cả series).
     * ⭐ Đây là method QUAN TRỌNG NHẤT - được RankingService sử dụng
     * để lấy dữ liệu tính toán bảng xếp hạng hàng tháng.
     *
     * @param month Tháng cần lấy ("YYYY-MM")
     * @return Danh sách metrics của tất cả series trong tháng đó
     */
    List<SeriesMetric> findByMonth(String month);

    /**
     * Kiểm tra xem đã có metric cho series + tháng nào đó chưa.
     * Dùng trong quá trình import để biết là insert hay update.
     *
     * @param seriesId ID của series
     * @param month    Tháng cần kiểm tra
     * @return true nếu đã tồn tại
     */
    boolean existsBySeriesIdAndMonth(Long seriesId, String month);

    // ═══════════════════════════════════════════════════════════
    //  DASHBOARD STATISTICS — Thêm cho Series Statistics Feature
    // ═══════════════════════════════════════════════════════════

    /**
     * Lấy top N series trong một tháng, sắp xếp theo compositeScore giảm dần.
     * Dùng trong ChiefDashboardService.topSeries(): lấy danh sách xếp hạng.
     *
     * @param month    Tháng cần lấy ("YYYY-MM")
     * @param pageable PageRequest với limit (vd: PageRequest.of(0, 10))
     * @return Danh sách SeriesMetric sắp xếp theo compositeScore DESC
     */
    @Query("SELECT sm FROM SeriesMetric sm WHERE sm.month = :month " +
           "ORDER BY sm.compositeScore DESC")
    List<SeriesMetric> findTopByMonth(@Param("month") String month, Pageable pageable);

    /**
     * Tìm metric gần nhất của 1 series (theo tháng mới nhất).
     * Dùng trong MangakaDashboardService.mySeries(): lấy composite score gần nhất.
     *
     * @param seriesId ID của series
     * @return Optional<SeriesMetric> — empty nếu series chưa có metric nào
     */
    Optional<SeriesMetric> findFirstBySeriesIdOrderByMonthDesc(Long seriesId);
}
