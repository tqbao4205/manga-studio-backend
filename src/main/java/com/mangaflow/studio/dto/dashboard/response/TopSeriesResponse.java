package com.mangaflow.studio.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSeriesResponse {

    private int rank;
    private Long seriesId;
    private String seriesTitle;
    private String mangakaName;
    private String tier;
    private long totalVotes;
    private double avgScore;
    private double compositeScore;

}
