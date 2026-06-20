package com.mangaflow.studio.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChiefOverviewResponse {

    private long totalSeries;
    private long ongoingSeries;
    private long atRiskSeries;
    private long completedSeries;
    private long totalPublishedChapters;
    private long chaptersThisMonth;
    private long newSeriesThisMonth;
    private long totalUsers;
    private long mangakaCount;
    private long assistantCount;
    private long tantouEditorCount;
    private long boardMemberCount;

}
