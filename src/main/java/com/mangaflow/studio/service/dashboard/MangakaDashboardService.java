package com.mangaflow.studio.service.dashboard;

import com.mangaflow.studio.common.exception.AppException;
import com.mangaflow.studio.dto.dashboard.response.*;
import com.mangaflow.studio.model.chapter.Chapter;
import com.mangaflow.studio.model.chapter.ChapterStatus;
import com.mangaflow.studio.model.metric.SeriesMetric;
import com.mangaflow.studio.model.series.InvitationStatus;
import com.mangaflow.studio.model.series.Series;
import com.mangaflow.studio.model.series.SeriesAssistant;
import com.mangaflow.studio.model.series.SeriesStatus;
import com.mangaflow.studio.repository.chapter.ChapterRepository;
import com.mangaflow.studio.repository.metric.SeriesMetricRepository;
import com.mangaflow.studio.repository.series.SeriesAssistantRepository;
import com.mangaflow.studio.repository.series.SeriesRepository;
import com.mangaflow.studio.repository.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MangakaDashboardService {

    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;
    private final SeriesMetricRepository seriesMetricRepository;
    private final SeriesAssistantRepository seriesAssistantRepository;
    private final TaskRepository taskRepository;

    /**
     * Tổng quan cá nhân của Mangaka.
     * Bước 1: Đếm series (tổng, đang PH) của mangaka
     * Bước 2: Đếm chapter (đã pub, đang làm, quá hạn)
     * Bước 3: Đếm assistant + pending invitations
     * Bước 4: Tìm tier và rank cao nhất
     */
    public MangakaOverviewResponse overview(Long mangakaId) {
        // ── Bước 1: Đếm series ──
        long totalSeries = seriesRepository.countByMangakaId(mangakaId);
        long ongoingSeries = seriesRepository.countByMangakaIdAndStatus(mangakaId, SeriesStatus.ONGOING);

        // ── Bước 2: Đếm chapter ──
        long totalPublishedChapters = chapterRepository.countPublishedByMangakaId(mangakaId);
        long inProgressCount = countChaptersByMangakaAndStatus(mangakaId, ChapterStatus.IN_PROGRESS);
        long overdueChapters = chapterRepository.countOverdueByMangakaId(mangakaId);

        // ── Bước 3: Đếm assistant ──
        long totalAssistants = seriesAssistantRepository.countAcceptedByMangakaId(mangakaId);
        long pendingInvitations = seriesAssistantRepository.countPendingByMangakaId(mangakaId);

        // ── Bước 4: Tìm tier/rank cao nhất ──
        List<Series> mySeries = seriesRepository.findByMangakaId(mangakaId);
        String bestTier = null;
        Long bestRank = null;

        List<String> tierOrder = List.of("S", "A", "B", "C", "D");
        for (Series s : mySeries) {
            if (s.getCurrentTier() != null) {
                if (bestTier == null || tierOrder.indexOf(s.getCurrentTier()) < tierOrder.indexOf(bestTier)) {
                    bestTier = s.getCurrentTier();
                }
            }
            if (s.getCurrentRank() != null) {
                if (bestRank == null || s.getCurrentRank() < bestRank) {
                    bestRank = Long.valueOf(s.getCurrentRank());
                }
            }
        }

        return MangakaOverviewResponse.builder()
                .totalSeries(totalSeries)
                .ongoingSeries(ongoingSeries)
                .totalPublishedChapters(totalPublishedChapters)
                .inProgressChapters(inProgressCount)
                .overdueChapters(overdueChapters)
                .totalAssistants(totalAssistants)
                .pendingInvitations(pendingInvitations)
                .bestTier(bestTier)
                .bestRank(bestRank)
                .build();
    }

    /**
     * Danh sách series kèm thông số thống kê của Mangaka.
     * Bước 1: Lấy tất cả series của mangaka
     * Bước 2: Với mỗi series, đếm chapter theo status
     * Bước 3: Đếm assistant của từng series
     * Bước 4: Lấy composite score gần nhất
     */
    public List<MySeriesStatResponse> getMySeries(Long mangakaId) {
        List<Series> seriesList = seriesRepository.findByMangakaId(mangakaId);
        List<MySeriesStatResponse> result = new ArrayList<>();

        for (Series s : seriesList) {
            int totalChapters = s.getChapterCount() != null ? s.getChapterCount() : 0;
            int publishedChapters = (int) chapterRepository.countBySeriesIdAndStatus(s.getId(), ChapterStatus.PUBLISHED);
            int inProgressChapters = (int) chapterRepository.countBySeriesIdAndStatus(s.getId(), ChapterStatus.IN_PROGRESS);
            int assistantCount = (int) seriesAssistantRepository.countBySeriesIdAndStatus(s.getId(), InvitationStatus.ACCEPTED);

            Double latestScore = seriesMetricRepository
                    .findFirstBySeriesIdOrderByMonthDesc(s.getId())
                    .map(SeriesMetric::getCompositeScore)
                    .orElse(null);

            result.add(MySeriesStatResponse.builder()
                    .seriesId(s.getId())
                    .title(s.getTitle())
                    .status(s.getStatus().name())
                    .currentTier(s.getCurrentTier())
                    .currentRank(s.getCurrentRank())
                    .totalChapters(totalChapters)
                    .publishedChapters(publishedChapters)
                    .inProgressChapters(inProgressChapters)
                    .assistantCount(assistantCount)
                    .latestCompositeScore(latestScore)
                    .build());
        }

        return result;
    }

    /**
     * Tiến độ chapter của một series — chỉ cho phép Mangaka của series đó xem.
     * Bước 1: Kiểm tra quyền sở hữu
     * Bước 2: Đếm chapter theo từng trạng thái
     * Bước 3: Lấy 5 chapter gần nhất
     */
    public ChapterProgressResponse getChapterProgress(Long seriesId, Long mangakaId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Series not found"));

        if (!series.getMangaka().getId().equals(mangakaId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not own this series");
        }

        int totalChapters = series.getChapterCount() != null ? series.getChapterCount() : 0;
        int publishedChapters = (int) chapterRepository.countBySeriesIdAndStatus(seriesId, ChapterStatus.PUBLISHED);
        int inProgressChapters = (int) chapterRepository.countBySeriesIdAndStatus(seriesId, ChapterStatus.IN_PROGRESS);
        int draftChapters = (int) chapterRepository.countBySeriesIdAndStatus(seriesId, ChapterStatus.DRAFT);

        List<Chapter> recentChapters = chapterRepository.findTop5BySeriesIdOrderByChapterNumberDesc(seriesId);
        LocalDate today = LocalDate.now();

        List<ChapterProgressResponse.ChapterSummary> summaries = recentChapters.stream()
                .map(ch -> ChapterProgressResponse.ChapterSummary.builder()
                        .chapterId(ch.getId())
                        .chapterNumber(ch.getChapterNumber())
                        .title(ch.getTitle())
                        .status(ch.getStatus().name())
                        .deadline(ch.getDeadline())
                        .overdue(ch.getDeadline() != null && ch.getDeadline().isBefore(today)
                                && ch.getStatus() != ChapterStatus.PUBLISHED)
                        .build())
                .toList();

        long overdueChapters = summaries.stream().filter(ChapterProgressResponse.ChapterSummary::isOverdue).count();

        return ChapterProgressResponse.builder()
                .seriesId(seriesId)
                .seriesTitle(series.getTitle())
                .totalChapters(totalChapters)
                .publishedChapters(publishedChapters)
                .inProgressChapters(inProgressChapters)
                .draftChapters(draftChapters)
                .overdueChapters((int) overdueChapters)
                .recentChapters(summaries)
                .build();
    }

    /**
     * Thông tin team của một series — chỉ Mangaka của series đó xem được.
     * Bước 1: Kiểm tra quyền sở hữu
     * Bước 2: Lấy danh sách assistant (ACCEPTED)
     * Bước 3: Với mỗi assistant, đếm task được giao / hoàn thành / từ chối
     * Bước 4: Tính task completion rate
     */
    public TeamStatResponse getTeamStat(Long seriesId, Long mangakaId) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Series not found"));

        if (!series.getMangaka().getId().equals(mangakaId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not own this series");
        }

        // Lấy danh sách assistant ACCEPTED
        List<SeriesAssistant> acceptedAssistants = seriesAssistantRepository
                .findBySeriesIdAndStatus(seriesId, InvitationStatus.ACCEPTED);

        // Lấy danh sách PENDING
        List<SeriesAssistant> pendingAssistants = seriesAssistantRepository
                .findBySeriesIdAndStatus(seriesId, InvitationStatus.PENDING);

        List<TeamStatResponse.AssistantSummary> summaries = new ArrayList<>();
        long totalAssigned = 0;
        long totalCompleted = 0;
        long totalRevised = 0;

        for (SeriesAssistant sa : acceptedAssistants) {
            Long assistantId = sa.getAssistant().getId();

            long assignedTasks = countTasksByAssistantAndSeries(assistantId, seriesId);
            long completedTasks = countCompletedTasksByAssistantAndSeries(assistantId, seriesId);
            long revisedTasks = countRevisedTasksByAssistantAndSeries(assistantId, seriesId);

            totalAssigned += assignedTasks;
            totalCompleted += completedTasks;
            totalRevised += revisedTasks;

            summaries.add(TeamStatResponse.AssistantSummary.builder()
                    .assistantId(assistantId)
                    .displayName(sa.getAssistant().getDisplayName())
                    .email(sa.getAssistant().getEmail())
                    .assignedTasks((int) assignedTasks)
                    .completedTasks((int) completedTasks)
                    .rejectedTasks((int) revisedTasks)
                    .build());
        }

        double completionRate = totalAssigned > 0
                ? (double) totalCompleted / totalAssigned * 100.0
                : 0.0;

        return TeamStatResponse.builder()
                .totalAssistants(acceptedAssistants.size() + pendingAssistants.size())
                .acceptedAssistants(acceptedAssistants.size())
                .pendingInvitations(pendingAssistants.size())
                .taskCompletionRate(Math.round(completionRate * 100.0) / 100.0)
                .assistants(summaries)
                .build();
    }

    // ── Helper: đếm chapter IN_PROGRESS của tất cả series của 1 mangaka ──
    private long countChaptersByMangakaAndStatus(Long mangakaId, ChapterStatus status) {
        List<Series> seriesList = seriesRepository.findByMangakaId(mangakaId);
        long count = 0;
        for (Series s : seriesList) {
            count += chapterRepository.countBySeriesIdAndStatus(s.getId(), status);
        }
        return count;
    }

    // ── Helper: đếm task đã giao cho assistant trong các region của series ──
    private long countTasksByAssistantAndSeries(Long assistantId, Long seriesId) {
        return taskRepository.countByAssistantIdAndSeriesId(assistantId, seriesId);
    }

    // ── Helper: đếm task COMPLETED (DONE) ──
    private long countCompletedTasksByAssistantAndSeries(Long assistantId, Long seriesId) {
        return taskRepository.countByAssistantIdAndSeriesIdAndStatus(
                assistantId, seriesId, com.mangaflow.studio.model.task.TaskStatus.DONE);
    }

    // ── Helper: đếm task cần sửa (REVISE) ──
    private long countRevisedTasksByAssistantAndSeries(Long assistantId, Long seriesId) {
        return taskRepository.countByAssistantIdAndSeriesIdAndStatus(
                assistantId, seriesId, com.mangaflow.studio.model.task.TaskStatus.REVISE);
    }
}
