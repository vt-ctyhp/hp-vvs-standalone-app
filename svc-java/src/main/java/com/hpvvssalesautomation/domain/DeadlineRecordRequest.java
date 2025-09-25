package com.hpvvssalesautomation.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class DeadlineRecordRequest {

    @NotBlank
    @JsonProperty("rootApptId")
    private String rootApptId;

    @NotBlank
    @JsonProperty("deadlineType")
    private String deadlineType;

    @NotBlank
    @JsonProperty("deadlineDate")
    private String deadlineDate;

    @NotBlank
    @JsonProperty("movedBy")
    private String movedBy;

    @JsonProperty("assistedRep")
    private String assistedRep;

    public String getRootApptId() {
        return rootApptId;
    }

    public void setRootApptId(String rootApptId) {
        this.rootApptId = rootApptId;
    }

    public String getDeadlineType() {
        return deadlineType;
    }

    public void setDeadlineType(String deadlineType) {
        this.deadlineType = deadlineType;
    }

    public String getDeadlineDate() {
        return deadlineDate;
    }

    public void setDeadlineDate(String deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    public String getMovedBy() {
        return movedBy;
    }

    public void setMovedBy(String movedBy) {
        this.movedBy = movedBy;
    }

    public String getAssistedRep() {
        return assistedRep;
    }

    public void setAssistedRep(String assistedRep) {
        this.assistedRep = assistedRep;
    }
}
