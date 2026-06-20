package com.mangaflow.studio.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MangakaOverviewResponse {

    private long totalSeries;
    private long ongoingSeries;
    private long totalPublishedChapters;
    private long inProgressChapters;
    private long overdueChapters;
    private long totalAssistants;
    private long pendingInvitations;
    private String bestTier;
    private Long bestRank;

}
