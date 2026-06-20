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
public class TeamStatResponse {

    private int totalAssistants;
    private int acceptedAssistants;
    private int pendingInvitations;
    private double taskCompletionRate;
    private List<AssistantSummary> assistants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssistantSummary {
        private Long assistantId;
        private String displayName;
        private String email;
        private int assignedTasks;
        private int completedTasks;
        private int rejectedTasks;
    }

}
