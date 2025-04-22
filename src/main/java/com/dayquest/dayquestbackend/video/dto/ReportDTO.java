package com.dayquest.dayquestbackend.video.dto;

import java.util.UUID;

public class ReportDTO {
    private UUID videoUuid;
    private UUID reporterUuid;
    private String reason;

    public ReportDTO() {
    }

    public ReportDTO(UUID videoUuid, UUID reporterUuid, String reason) {
        this.videoUuid = videoUuid;
        this.reporterUuid = reporterUuid;
        this.reason = reason;
    }

    public UUID getVideoUuid() {
        return videoUuid;
    }

    public void setVideoUuid(UUID videoUuid) {
        this.videoUuid = videoUuid;
    }

    public UUID getReporterUuid() {
        return reporterUuid;
    }

    public void setReporterUuid(UUID reporterUuid) {
        this.reporterUuid = reporterUuid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
