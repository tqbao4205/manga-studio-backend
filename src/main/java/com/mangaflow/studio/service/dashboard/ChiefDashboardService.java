package com.mangaflow.studio.service.dashboard;

import com.mangaflow.studio.dto.dashboard.response.*;
import com.mangaflow.studio.model.auth.Role;
import com.mangaflow.studio.model.chapter.ChapterStatus;
import com.mangaflow.studio.model.metric.SeriesMetric;
import com.mangaflow.studio.model.series.Series;
import com.mangaflow.studio.model.series.SeriesStatus;
import com.mangaflow.studio.repository.auth.UserRepository;
import com.mangaflow.studio.repository.chapter.ChapterRepository;
import com.mangaflow.studio.repository.metric.SeriesMetricRepository;
import com.mangaflow.studio.repository.series.SeriesRepository;
import com.mangaflow.studio.repository.series.SeriesTantouInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChiefDashboardService {

    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final SeriesMetricRepository seriesMetricRepository;
    private final SeriesTantouInvitationRepository tantouInvitationRepository;

    /**
     * Tổng quan nền tảng — đếm tất cả các chỉ số chính.
     * Bước 1: Đếm series (total, ongoing, at_risk, completed)
     * Bước 2: Đếm chapter (tổng published, published tháng này)
     * Bước 3: Đếm series mới tạo trong tháng
     * Bước 4: Đếm user (total, theo từng role)
     * Bước 5: Build response
     */
    public ChiefOverviewResponse overview() {
        // ── Bước 1: Đếm series ──
        long totalSeries = seriesRepository.count();
        long ongoingSeries = seriesRepository.countByStatus(SeriesStatus.ONGOING);
        long atRiskSeries = seriesRepository.countByStatus(SeriesStatus.AT_RISK);
        long completedSeries = seriesRepository.countByStatus(SeriesStatus.COMPLETED);

        // ── Bước 2: Đếm chapter ──
        long totalPublishedChapters = chapterRepository.countByStatus(ChapterStatus.PUBLISHED);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        long chaptersThisMonth = chapterRepository.countByStatusAndPublishDateBetween(
                ChapterStatus.PUBLISHED, startOfMonth, endOfMonth);

        // ── Bước 3: Series mới trong tháng ──
        long newSeriesThisMonth = seriesRepository.countByCreatedAtBetween(startOfMonth, LocalDateTime.now());

        // ── Bước 4: Đếm user ──
        long totalUsers = userRepository.count();
        long mangakaCount = userRepository.countByRole(Role.MANGAKA);
        long assistantCount = userRepository.countByRole(Role.ASSISTANT);
        long tantouEditorCount = userRepository.countByRole(Role.TANTOU_EDITOR);
        long boardMemberCount = userRepository.countByRole(Role.EDITORIAL_BOARD)
                              + userRepository.countByRole(Role.CHIEF_EDITOR);

        // ── Bước 5: Build response ──
        return ChiefOverviewResponse.builder()
                .totalSeries(totalSeries)
                .ongoingSeries(ongoingSeries)
                .atRiskSeries(atRiskSeries)
                .completedSeries(completedSeries)
                .totalPublishedChapters(totalPublishedChapters)
                .chaptersThisMonth(chaptersThisMonth)
                .newSeriesThisMonth(newSeriesThisMonth)
                .totalUsers(totalUsers)
                .mangakaCount(mangakaCount)
                .assistantCount(assistantCount)
                .tantouEditorCount(tantouEditorCount)
                .boardMemberCount(boardMemberCount)
                .build();
    }

    /**
     * Phân bố series theo trạng thái — dùng cho biểu đồ tròn.
     * Bước 1: Gọi GROUP BY status từ DB
     * Bước 2: Map kết quả vào SeriesStatusSummary kèm label tiếng Việt
     */
    public List<SeriesStatusSummary> getSeriesByStatus() {
        List<Object[]> results = seriesRepository.countByStatusGrouped();
        List<SeriesStatusSummary> list = new ArrayList<>();

        for (Object[] row : results) {
            SeriesStatus status = (SeriesStatus) row[0];
            long count = (Long) row[1];
            list.add(SeriesStatusSummary.builder()
                    .status(status.name())
                    .label(getStatusLabel(status))
                    .count(count)
                    .build());
        }

        return list;
    }

    /**
     * Phân bố tier S/A/B/C/D — dùng cho biểu đồ cột.
     * Bước 1: Gọi GROUP BY tier từ DB
     * Bước 2: Map kết quả vào TierDistributionResponse
     * Bước 3: Tính unranked = total series - sum các tier đã đếm
     */
    public TierDistributionResponse getTierDistribution() {
        List<Object[]> results = seriesRepository.countByTierGrouped();
        long totalSeries = seriesRepository.count();

        long tierS = 0, tierA = 0, tierB = 0, tierC = 0, tierD = 0;
        long sum = 0;

        for (Object[] row : results) {
            String tier = (String) row[0];
            long count = (Long) row[1];
            sum += count;

            switch (tier) {
                case "S" -> tierS = count;
                case "A" -> tierA = count;
                case "B" -> tierB = count;
                case "C" -> tierC = count;
                case "D" -> tierD = count;
            }
        }

        long unranked = totalSeries - sum;

        return TierDistributionResponse.builder()
                .tierS(tierS)
                .tierA(tierA)
                .tierB(tierB)
                .tierC(tierC)
                .tierD(tierD)
                .unranked(unranked)
                .build();
    }

    /**
     * Top N series theo composite score trong một tháng.
     * Bước 1: Parse month, nếu null → dùng tháng hiện tại
     * Bước 2: Query SeriesMetricRepository với limit
     * Bước 3: Map kết quả kèm thông tin series + mangaka
     * Bước 4: Gán rank = index + 1
     */
    public List<TopSeriesResponse> getTopSeries(Integer limit, String month) {
        if (month == null) {
            month = YearMonth.now().toString();
        }
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<SeriesMetric> topMetrics = seriesMetricRepository.findTopByMonth(
                month, PageRequest.of(0, limit));

        List<TopSeriesResponse> result = new ArrayList<>();
        int rank = 1;
        for (SeriesMetric metric : topMetrics) {
            Series series = metric.getSeries();
            result.add(TopSeriesResponse.builder()
                    .rank(rank++)
                    .seriesId(series.getId())
                    .seriesTitle(series.getTitle())
                    .mangakaName(series.getMangaka().getDisplayName())
                    .tier(metric.getTier())
                    .totalVotes(metric.getTotalVotes())
                    .avgScore(metric.getAvgScore())
                    .compositeScore(metric.getCompositeScore())
                    .build());
        }

        return result;
    }

    /**
     * Danh sách series đang bị cảnh báo — cần Chief theo dõi.
     * Bước 1: Lọc series AT_RISK
     * Bước 2: Lọc series ONGOING có warning 1-2 tháng
     * Bước 3: Kết hợp và sắp xếp theo mức độ nghiêm trọng
     */
    public List<Series> getAtRiskSeries() {
        List<Series> atRisk = seriesRepository.findByStatusIn(
                List.of(SeriesStatus.AT_RISK));

        List<Series> warningOngoing = seriesRepository.findAll().stream()
                .filter(s -> s.getStatus() == SeriesStatus.ONGOING
                        && s.getConsecutiveWarningMonths() != null
                        && s.getConsecutiveWarningMonths() >= 1)
                .toList();

        Set<Series> combined = new LinkedHashSet<>();
        combined.addAll(atRisk);
        combined.addAll(warningOngoing);

        return combined.stream()
                .sorted(Comparator.comparing(
                        s -> s.getConsecutiveWarningMonths() != null
                                ? s.getConsecutiveWarningMonths() : 0,
                        Comparator.reverseOrder()))
                .toList();
    }

    /**
     * Công việc cần xử lý — "hộp thư đến" cho Chief.
     * Bước 1: Đếm series chờ tantou accept
     * Bước 2: Đếm series chờ board vote
     * Bước 3: Đếm series cần cancel decision (AT_RISK ≥ 3 tháng)
     * Bước 4: Đếm chapter chờ publish approval
     */
    public PendingActionsResponse getPendingActions() {
        long pendingTantouSeries = tantouInvitationRepository.countByStatus(
                com.mangaflow.studio.model.series.InvitationStatus.PENDING);

        long pendingBoardVoteSeries = seriesRepository.countByStatus(SeriesStatus.PENDING_BOARD_VOTE);

        long pendingCancelDecisions = seriesRepository.findAll().stream()
                .filter(s -> s.getStatus() == SeriesStatus.AT_RISK
                        && s.getConsecutiveWarningMonths() != null
                        && s.getConsecutiveWarningMonths() >= 3)
                .count();

        long pendingChapterApprovals = chapterRepository.countByStatus(ChapterStatus.PENDING_BOARD_APPROVAL);

        long total = pendingTantouSeries + pendingBoardVoteSeries
                   + pendingCancelDecisions + pendingChapterApprovals;

        return PendingActionsResponse.builder()
                .pendingTantouSeries(pendingTantouSeries)
                .pendingBoardVoteSeries(pendingBoardVoteSeries)
                .pendingCancelDecisions(pendingCancelDecisions)
                .pendingChapterApprovals(pendingChapterApprovals)
                .totalPending(total)
                .build();
    }

    // ── Helper: map SeriesStatus → label tiếng Việt ──
    private String getStatusLabel(SeriesStatus status) {
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case PENDING_TANTOU -> "Chờ Tantou duyệt";
            case PENDING_BOARD_VOTE -> "Chờ Board vote";
            case ONGOING -> "Đang phát hành";
            case HIATUS -> "Tạm ngưng";
            case CANCELLED -> "Đã hủy";
            case COMPLETED -> "Hoàn thành";
            case AT_RISK -> "Nguy cơ bị hủy";
            case PENDING_APPROVAL -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
        };
    }
}
