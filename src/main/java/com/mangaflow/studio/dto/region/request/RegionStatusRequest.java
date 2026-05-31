package com.mangaflow.studio.dto.region.request;

import com.mangaflow.studio.model.region.RegionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request đổi trạng thái region")
public class RegionStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Trạng thái mới: PENDING, IN_PROGRESS, COMPLETED", example = "IN_PROGRESS")
    private RegionStatus status;
}
