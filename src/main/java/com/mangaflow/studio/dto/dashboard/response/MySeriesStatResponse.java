package com.mangaflow.studio.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MySeriesStatResponse {

    private Long seriesId;
    private String title;
    private String status;
    private String currentTier;
    private Integer currentRank;
    private int totalChapters;
    private int publishedChapters;
    private int inProgressChapters;
    private int assistantCount;
    private Double latestCompositeScore;

}
