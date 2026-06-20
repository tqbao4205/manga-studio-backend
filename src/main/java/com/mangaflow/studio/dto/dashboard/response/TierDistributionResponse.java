package com.mangaflow.studio.dto.dashboard.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierDistributionResponse {

    private long tierS;
    private long tierA;
    private long tierB;
    private long tierC;
    private long tierD;
    private long unranked;

}
