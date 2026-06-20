package com.mangaflow.studio.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingActionsResponse {

    private long pendingTantouSeries;
    private long pendingBoardVoteSeries;
    private long pendingCancelDecisions;
    private long pendingChapterApprovals;
    private long totalPending;

}
