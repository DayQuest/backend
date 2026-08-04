package com.dayquest.contentservice.dto;

import com.dayquest.contentservice.model.ReportReason;
import com.dayquest.contentservice.model.ReportType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;


@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class CreateReportRequest {

    private String description;

    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    @NotNull(message = "Report type is required")
    private ReportType type;

    @NotNull(message = "Report reason is required")
    private ReportReason reason;

}

