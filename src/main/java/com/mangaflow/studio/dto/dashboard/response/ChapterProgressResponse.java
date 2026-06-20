package com.mangaflow.studio.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterProgressResponse {

    private Long seriesId;
    private String seriesTitle;
    private int totalChapters;
    private int publishedChapters;
    private int inProgressChapters;
    private int draftChapters;
    private int overdueChapters;
    private List<ChapterSummary> recentChapters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterSummary {
        private Long chapterId;
        private int chapterNumber;
        private String title;
        private String status;
        private java.time.LocalDate deadline;
        private boolean overdue;
    }

}
